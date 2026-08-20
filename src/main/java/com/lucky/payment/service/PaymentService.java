package com.lucky.payment.service;

import com.lucky.fortune.domain.Fortune;
import com.lucky.fortune.service.FortuneService;
import com.lucky.payment.client.PortOneApiClient;
import com.lucky.payment.client.PortOnePayment;
import com.lucky.payment.config.PortOneProperties;
import com.lucky.payment.domain.Payment;
import com.lucky.payment.dto.PaymentResult;
import com.lucky.payment.dto.PreparePaymentRequest;
import com.lucky.payment.dto.PreparePaymentResponse;
import com.lucky.payment.dto.UnfulfilledPayment;
import com.lucky.payment.mapper.PaymentMapper;
import com.lucky.payment.webhook.PortOneWebhookVerifier;
import com.lucky.payment.webhook.WebhookPayload;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** 포트원 V2 결제: 준비 / 검증(완료) / 웹훅 처리. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final PaymentMapper paymentMapper;
    private final PortOneApiClient portOneApiClient;
    private final PortOneWebhookVerifier webhookVerifier;
    private final PortOneProperties props;
    private final FortuneService fortuneService;

    /**
     * 결제 준비: 서버가 paymentId 를 생성하고 PENDING 으로 저장 → 프론트가 이 id 로 결제창 호출.
     * 가격·상품명은 클라이언트 입력이 아니라 fortunes(slug)에서 가져온다(금액 위조 불가).
     */
    @Transactional
    public PreparePaymentResponse prepare(Long memberId, PreparePaymentRequest req) {
        Fortune fortune = fortuneService.getActiveEntity(req.slug());  // 없거나 비활성이면 404
        // 0원 상품은 PG(포트원)가 결제를 거부한다 → 결제창에서 막히고 쓸모없는 PENDING 행만 남는다.
        // 애초에 여기서 끊어 사용자에게 명확히 알린다(운영: admin 에서 가격 1원 이상으로 저장하도록 강제).
        if (fortune.getPrice() <= 0) {
            log.warn("[결제 준비] 0원 상품 결제 시도 slug={}", req.slug());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "현재 결제할 수 없는 상품입니다. 잠시 후 다시 시도해 주세요.");
        }
        String paymentId = "saju_" + UUID.randomUUID().toString().replace("-", "");
        String inputJson = null;
        if (req.input() != null) {
            try {
                inputJson = JSON.writeValueAsString(req.input());
            } catch (Exception e) {
                log.warn("[결제 준비] 입력 스냅샷 직렬화 실패 slug={}", req.slug(), e);
            }
        }
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .memberId(memberId)
                .productCode(fortune.getSlug())
                .orderName(fortune.getTitle())
                .amount(fortune.getPrice())
                .currency("KRW")
                .status("PENDING")
                .inputJson(inputJson)
                .build();
        paymentMapper.insert(payment);
        return new PreparePaymentResponse(paymentId, fortune.getTitle(), fortune.getPrice(), "KRW", props.storeId());
    }

    /** 결제 완료됐지만 리포트가 없는 결제건(보관함 복구 안내용). */
    public List<UnfulfilledPayment> listUnfulfilled(Long memberId) {
        return paymentMapper.findUnfulfilledByMember(memberId).stream()
                .map(p -> new UnfulfilledPayment(
                        p.getPaymentId(), p.getProductCode(), p.getOrderName(),
                        p.getAmount(), p.getPaidAt(),
                        p.getInputJson() != null && !p.getInputJson().isBlank()))
                .toList();
    }

    /** 복구 생성에 쓸 결제건(본인·PAID·입력 보관 확인). */
    public Payment requireResumable(Long memberId, String paymentId) {
        Payment p = paymentMapper.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        if (p.getMemberId() == null || !p.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 결제건이 아닙니다.");
        }
        if (!"PAID".equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "결제가 완료되지 않았습니다.");
        }
        if (p.getInputJson() == null || p.getInputJson().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "저장된 입력이 없어 자동으로 만들 수 없어요. 고객센터로 문의해 주세요.");
        }
        return p;
    }

    /**
     * 결제 완료 검증: 프론트 requestPayment 성공 후 호출.
     * 포트원에서 실제 결제건을 조회해 금액 일치 확인 후 상태를 저장한다.
     */
    @Transactional
    public PaymentResult complete(Long memberId, String paymentId) {
        Payment stored = paymentMapper.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        if (stored.getMemberId() != null && !stored.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 결제건이 아닙니다.");
        }
        return sync(stored);
    }

    /**
     * 결제 취소(환불). 리포트 생성이 끝내 실패했을 때 서버가 스스로 호출한다.
     *
     * <p>PAID 가 아니면 아무 것도 하지 않는다(이미 취소됐거나 결제 전). 환불에 성공하면
     * payments.status 를 CANCELLED 로 내리고, 그 시점부터 해당 리포트는 열람이 차단된다
     * (FortuneResultService.getMine 이 payments.status 를 함께 본다).
     *
     * @return 실제로 환불이 이루어졌으면 true
     */
    @Transactional
    public boolean refund(String paymentId, String reason) {
        Payment p = paymentMapper.findByPaymentId(paymentId).orElse(null);
        if (p == null) {
            log.warn("[자동 환불] 결제건 없음 paymentId={}", paymentId);
            return false;
        }
        if (!"PAID".equals(p.getStatus())) {
            log.info("[자동 환불] PAID 아님 → 건너뜀 paymentId={} status={}", paymentId, p.getStatus());
            return false;
        }
        portOneApiClient.cancel(paymentId, reason);
        p.setStatus("CANCELLED");
        paymentMapper.updateResult(p);
        log.info("[자동 환불] 완료 paymentId={} amount={} reason={}", paymentId, p.getAmount(), reason);
        return true;
    }

    /** entitlement: 이 회원이 이 컨텐츠(slug)를 결제 완료(PAID)한 적이 있는지. */
    public boolean hasPaid(Long memberId, String productCode) {
        return paymentMapper.existsPaidByMemberAndProduct(memberId, productCode);
    }

    /**
     * 풀 리포트 생성 자격 검증.
     * 해당 paymentId 가 (1)이 회원의 결제건이고 (2)결제 대상이 slug 와 일치하며 (3)PAID 여야 한다.
     */
    public void assertPaidFor(Long memberId, String paymentId, String slug) {
        Payment p = paymentMapper.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));
        if (p.getMemberId() == null || !p.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 결제건이 아닙니다.");
        }
        if (!slug.equals(p.getProductCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제한 컨텐츠와 다릅니다.");
        }
        if (!"PAID".equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "결제가 완료되지 않았습니다.");
        }
    }

    /**
     * 웹훅 처리: 서명 검증 → paymentId 추출 → 포트원 재조회로 검증/저장.
     * (클라이언트 complete 호출이 누락돼도 결제 상태를 확실히 반영하기 위한 안전장치)
     */
    public void handleWebhook(String rawBody, String webhookId, String webhookTimestamp, String webhookSignature) {
        webhookVerifier.verify(rawBody, webhookId, webhookTimestamp, webhookSignature);

        WebhookPayload payload;
        try {
            payload = JSON.readValue(rawBody, WebhookPayload.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "웹훅 본문 파싱 실패", e);
        }

        String paymentId = payload.data() != null ? payload.data().paymentId() : null;
        if (paymentId == null) {
            log.info("[PortOne Webhook] paymentId 없는 이벤트 무시: type={}", payload.type());
            return;
        }

        paymentMapper.findByPaymentId(paymentId).ifPresentOrElse(
                this::sync,
                () -> log.warn("[PortOne Webhook] 미등록 결제건(prepare 누락?) paymentId={}", paymentId));
    }

    /** 포트원 재조회 → 금액 검증 → 상태/원본 저장. complete·webhook 공통 로직. */
    private PaymentResult sync(Payment stored) {
        String rawJson = portOneApiClient.getPaymentRaw(stored.getPaymentId());
        PortOnePayment remote;
        try {
            remote = JSON.readValue(rawJson, PortOnePayment.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "포트원 응답 파싱 실패", e);
        }

        // 금액 위변조 검증: 결제건이 PAID 일 때 기대 금액과 실제 결제 금액이 일치해야 함
        if (remote.isPaid() && remote.totalAmount() != stored.getAmount()) {
            log.error("[결제 위변조 의심] paymentId={} 기대={} 실제={}",
                    stored.getPaymentId(), stored.getAmount(), remote.totalAmount());
            stored.setStatus("FAILED");
            stored.setRawJson(rawJson);
            paymentMapper.updateResult(stored);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액 불일치(위변조 의심)");
        }

        stored.setStatus(remote.status());
        stored.setPaidAt(remote.paidAt());
        stored.setRawJson(rawJson);
        paymentMapper.updateResult(stored);

        // ⚠️ 실제 상품/혜택 지급은 여기서 remote.isPaid() == true 일 때, 검증된 금액 기준으로 처리하세요.
        return new PaymentResult(stored.getPaymentId(), remote.status(), remote.totalAmount(), remote.isPaid());
    }
}
