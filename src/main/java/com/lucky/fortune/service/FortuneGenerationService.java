package com.lucky.fortune.service;

import com.lucky.fortune.domain.Fortune;
import com.lucky.fortune.domain.FortuneResult;
import com.lucky.fortune.dto.EnqueueResponse;
import com.lucky.fortune.dto.GenerateRequest;
import com.lucky.fortune.event.FortuneGenerationEnqueuedEvent;
import com.lucky.fortune.mapper.FortuneResultMapper;
import com.lucky.fortune.saju.SajuFactsBuilder;
import com.lucky.payment.service.PaymentService;
import com.lucky.saju.client.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 운세 생성.
 *
 * <p>설계: 숫자·일주 등 '사실(facts)'은 {@link SajuFactsBuilder}가 결정적으로 계산하고,
 * AI(프롬프트)는 해석 텍스트/키워드만 만든다. 결과 = AI출력 ∪ facts(우리 값 우선).
 * 이러면 맛보기와 풀의 숫자가 항상 일치하고, 일주가 부정확해지지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FortuneGenerationService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** 맛보기는 짧고 싸게(도입부만). */
    private static final int TEASER_MAX_TOKENS = 600;
    /** 풀 리포트는 길게(15개 풀이 본문, 풀이당 500자+). 풀이당 길이를 늘리면 이 값도 올려야 잘림(JSON 깨짐) 방지. */
    private static final int FULL_MAX_TOKENS = 18000;

    /**
     * 생성 시도 한도(최초 1회 + 재시도 2회). 여기에 도달하면 더 이상 재시도를 받지 않고
     * 결제 금액을 자동으로 환불한다 — 돈은 받았는데 리포트는 못 주는 상태를 남기지 않기 위함.
     */
    static final int MAX_ATTEMPTS = 3;

    private final FortuneService fortuneService;
    private final LlmClient llmClient;
    private final PaymentService paymentService;
    private final FortuneResultMapper fortuneResultMapper;
    private final SajuFactsBuilder factsBuilder;
    private final ApplicationEventPublisher eventPublisher;

    /** 무료 맛보기: facts 계산 + 도입부(짧은 AI). 저장하지 않는다. */
    public JsonNode generateTeaser(String slug, GenerateRequest req) {
        Fortune fortune = fortuneService.getActiveEntity(slug);
        String prompt = requirePrompt(fortune.getTeaserPrompt(), "맛보기");
        JsonNode facts = factsBuilder.build(req.subjects());
        String userMsg = GenerationPrompt.user(req, facts, null);
        JsonNode ai = parse(llmClient.complete(prompt, userMsg, TEASER_MAX_TOKENS));
        return merge(ai, facts);
    }

    /**
     * 유료 풀 리포트 <b>생성 요청</b>(폴링 방식). 즉시 반환하고 실제 AI 생성은 백그라운드에서 돈다.
     *
     * <p>흐름: PAID 검증 → GENERATING 행 INSERT(payment_id UNIQUE 라 더블탭/재시도 멱등)
     * → 커밋 후 워커가 생성 시작(AFTER_COMMIT 이벤트). 이미 있던 결제건이면 그 행 상태를 그대로 반환.
     * AI 호출(최대 ~60초)은 요청 수명과 분리되므로 nginx/클라이언트 타임아웃에 안 걸린다.
     */
    @Transactional
    public EnqueueResponse enqueueFull(String slug, String paymentId, GenerateRequest input,
                                       String intro, Long memberId) {
        Fortune fortune = fortuneService.getActiveEntity(slug);     // 컨텐츠 유효성 검증
        paymentService.assertPaidFor(memberId, paymentId, slug);    // 결제 안 했으면 차단

        var existing = fortuneResultMapper.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            // 이미 생성됨/생성 중 → 그 상태 그대로(재요청·더블탭 멱등). AI 재호출 X.
            FortuneResult r = existing.get();
            return new EnqueueResponse(r.getId(), r.getStatus());
        }

        String name = input.subjects().isEmpty() ? null : input.subjects().get(0).name();
        FortuneResult row = FortuneResult.builder()
                .memberId(memberId)
                .slug(slug)
                // 상품명은 지금 값을 박아 둔다. 나중에 상품명·slug 이 바뀌어도 보관함에는
                // 손님이 산 그때 이름이 그대로 남아야 한다.
                .title(fortune.getTitle())
                .paymentId(paymentId)
                .name(name)
                .status("GENERATING")
                .inputJson(serializeInput(input, intro))
                .build();

        int inserted = fortuneResultMapper.insertGenerating(row);
        if (inserted == 0) {
            // 동시 요청이 같은 결제건을 방금 INSERT(ON CONFLICT DO NOTHING) → 그 행 상태 반환(멱등).
            return fortuneResultMapper.findByPaymentId(paymentId)
                    .map(r -> new EnqueueResponse(r.getId(), r.getStatus()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "결과 생성 요청이 충돌했습니다. 잠시 후 보관함에서 확인해 주세요."));
        }

        // 커밋 후 워커가 GENERATING 행을 읽어 생성 시작.
        eventPublisher.publishEvent(new FortuneGenerationEnqueuedEvent(row.getId()));
        return new EnqueueResponse(row.getId(), "GENERATING");
    }

    /**
     * 실패한(FAILED) 풀 리포트 재시도. 본인 것만. enqueue 때 저장한 input_json 으로 재생성.
     * DONE/GENERATING 이면 현재 상태를 그대로 반환(중복 생성 방지).
     */
    @Transactional
    public EnqueueResponse retry(Long id, Long memberId) {
        FortuneResult row = fortuneResultMapper.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "결과를 찾을 수 없습니다."));
        if (FortuneResultService.isRevoked(row.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "환불이 완료된 결제건이라 다시 만들 수 없어요.");
        }
        if (!"FAILED".equals(row.getStatus())) {
            return new EnqueueResponse(id, row.getStatus());   // 이미 완료/진행중 → 그대로
        }
        if (row.getAttempts() >= MAX_ATTEMPTS) {
            // 한도 도달분은 이미 자동 환불 처리됨 → 무한 재시도로 AI 비용이 새는 것을 막는다.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "여러 번 시도했지만 만들지 못했어요. 결제 금액은 환불 처리되며, "
                            + "불편을 드려 죄송합니다. 고객센터로 문의해 주세요.");
        }
        if (row.getInputJson() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "재생성에 필요한 정보가 없어 다시 만들 수 없습니다.");
        }
        fortuneResultMapper.markGenerating(id);
        eventPublisher.publishEvent(new FortuneGenerationEnqueuedEvent(id));
        return new EnqueueResponse(id, "GENERATING");
    }

    /**
     * 워커가 호출(백그라운드 스레드). AI 호출은 트랜잭션 밖에서 길게,
     * 완료/실패 UPDATE 만 단일 문장으로 짧게(autocommit) → DB 커넥션을 AI 도는 동안 잡지 않는다.
     * 절대 예외를 밖으로 던지지 않는다(워커 스레드라 받아줄 곳이 없음) → 항상 DONE/FAILED 로 마감.
     */
    public void runGeneration(Long resultId) {
        FortuneResult row = fortuneResultMapper.findById(resultId).orElse(null);
        if (row == null) {
            log.warn("생성 대상 행 없음 id={}", resultId);
            return;
        }
        if (!"GENERATING".equals(row.getStatus())) {
            // 중복 이벤트/이미 처리됨 → 무시(멱등).
            return;
        }
        try {
            Fortune fortune = fortuneService.getActiveEntity(row.getSlug());
            String prompt = requirePrompt(fortune.getFullPrompt(), "풀 리포트");

            JsonNode payload = JSON.readTree(row.getInputJson());
            GenerateRequest input = JSON.treeToValue(payload.get("input"), GenerateRequest.class);
            String intro = payload.hasNonNull("intro") ? payload.get("intro").asText() : null;

            JsonNode facts = factsBuilder.build(input.subjects());
            String userMsg = GenerationPrompt.user(input, facts, intro);
            JsonNode ai = parse(llmClient.complete(prompt, userMsg, FULL_MAX_TOKENS));
            JsonNode merged = merge(ai, facts);

            fortuneResultMapper.markDone(resultId, merged.toString());
            log.info("풀 리포트 생성 완료 id={}", resultId);
        } catch (Exception e) {
            log.warn("풀 리포트 생성 실패 id={}", resultId, e);
            int attempt = row.getAttempts() + 1;   // markFailed 가 올릴 값
            String msg = (e instanceof ResponseStatusException rse && rse.getReason() != null)
                    ? rse.getReason()
                    : "생성 중 오류가 발생했습니다. 다시 시도해 주세요.";
            if (attempt >= MAX_ATTEMPTS) {
                msg = finalizeWithRefund(row, attempt);
            }
            fortuneResultMapper.markFailed(resultId, msg);
        }
    }

    /**
     * 시도 한도에 도달한 실패 건: 결제 금액을 자동 환불하고 사용자에게 보여줄 안내 문구를 만든다.
     *
     * <p>환불에 실패해도 예외를 밖으로 던지지 않는다 — 워커 스레드라 받아줄 곳이 없고,
     * 최소한 FAILED 마감은 반드시 되어야 하기 때문. 이 경우 로그로 남겨 수동 처리한다.
     */
    private String finalizeWithRefund(FortuneResult row, int attempt) {
        try {
            boolean refunded = paymentService.refund(
                    row.getPaymentId(), "리포트 생성 " + attempt + "회 실패에 따른 자동 환불");
            if (refunded) {
                return "여러 번 시도했지만 리포트를 만들지 못했어요. 결제하신 금액은 자동으로 환불 처리했습니다. "
                        + "불편을 드려 죄송합니다.";
            }
            return "여러 번 시도했지만 리포트를 만들지 못했어요. 고객센터로 문의해 주시면 바로 도와드릴게요.";
        } catch (Exception e) {
            log.error("[자동 환불 실패] 수동 처리 필요 resultId={} paymentId={}",
                    row.getId(), row.getPaymentId(), e);
            return "여러 번 시도했지만 리포트를 만들지 못했어요. 환불 처리를 위해 고객센터로 문의해 주세요.";
        }
    }

    /**
     * 좀비 정리: 배포/장애로 워커가 죽어 GENERATING 인 채 방치된 행을 FAILED 로 마감한다.
     * 그래야 사용자가 무한 로딩에 갇히지 않고 재시도 버튼을 볼 수 있다.
     * 한도에 도달했다면 여기서도 자동 환불이 걸린다.
     */
    public int failStuckGenerating(java.time.OffsetDateTime before) {
        var stuck = fortuneResultMapper.findStuckGenerating(before);
        for (FortuneResult row : stuck) {
            int attempt = row.getAttempts() + 1;
            String msg = "생성이 중간에 멈췄어요. 다시 시도해 주세요.";
            if (attempt >= MAX_ATTEMPTS) {
                msg = finalizeWithRefund(row, attempt);
            }
            fortuneResultMapper.markFailed(row.getId(), msg);
            log.warn("[좀비 정리] GENERATING → FAILED id={} startedAt={}", row.getId(), row.getStartedAt());
        }
        return stuck.size();
    }

    /** 재시도용 입력 직렬화: {"input":GenerateRequest, "intro":..}. */
    private String serializeInput(GenerateRequest input, String intro) {
        ObjectNode payload = JSON.createObjectNode();
        payload.set("input", JSON.valueToTree(input));
        payload.put("intro", intro);
        return payload.toString();
    }

    /** AI 출력(JSON) + 결정적 facts 를 한 객체로 합친다(facts 키는 항상 우리 값). */
    private JsonNode merge(JsonNode ai, JsonNode facts) {
        ObjectNode result = JSON.createObjectNode();
        if (ai instanceof ObjectNode obj) {
            result.setAll(obj);
        } else {
            result.set("ai", ai);   // 객체가 아니면 통째로 보존
        }
        result.set("facts", facts);
        return result;
    }

    private String requirePrompt(String prompt, String what) {
        if (prompt == null || prompt.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    what + "가 준비되지 않은 컨텐츠입니다.");
        }
        return prompt;
    }

    /** AI 응답 문자열을 JSON 트리로 파싱. response_format=json_object라 정상이면 항상 유효 JSON. */
    private JsonNode parse(String json) {
        try {

            log.info(json);
            return JSON.readTree(json);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI가 올바른 형식(JSON)으로 응답하지 않았습니다.");
        }
    }
}
