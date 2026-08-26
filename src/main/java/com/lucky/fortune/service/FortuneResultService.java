package com.lucky.fortune.service;

import com.lucky.fortune.domain.FortuneResult;
import com.lucky.fortune.dto.MyResultSummary;
import com.lucky.fortune.dto.ResultDetailResponse;
import com.lucky.fortune.mapper.FortuneResultMapper;
import com.lucky.review.mapper.ReportRatingMapper;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 보관함: 결제 완료 후 저장된 풀 리포트 재조회.
 * 생성/저장은 {@link FortuneGenerationService#enqueueFull}이 담당하고, 여기선 읽기·삭제만.
 */
@Service
@RequiredArgsConstructor
public class FortuneResultService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final FortuneResultMapper resultMapper;
    /** 리포트 하단 별점. 이미 남겼는지 알려줘야 화면이 잠긴다(리포트당 1회). */
    private final ReportRatingMapper ratingMapper;

    /** 내 보관함 목록 (최신순, 본문 제외). */
    public List<MyResultSummary> listMine(Long memberId) {
        return resultMapper.findSummariesByMemberId(memberId);
    }

    /**
     * 제공 기간이 끝난 건의 본문·입력값 파기(스케줄러가 호출).
     * 열람 차단은 읽는 시점 판단이라 이 배치와 무관하게 이미 걸려 있고,
     * 여기서는 실제 데이터를 지우는 일만 한다.
     */
    @Transactional
    public int purgeExpired() {
        return resultMapper.purgeExpired(OffsetDateTime.now());
    }

    /**
     * 환불(취소)된 결제인지. payments.status 기준.
     * PortOne 은 전액 취소를 CANCELLED, 부분 취소를 PARTIAL_CANCELLED 로 준다.
     */
    public static boolean isRevoked(String paymentStatus) {
        return "CANCELLED".equals(paymentStatus) || "PARTIAL_CANCELLED".equals(paymentStatus);
    }

    /**
     * 콘텐츠 제공 기간(결제일 + 1년)이 지났는지. 약관 제9조 · 환불정책 제2조.
     * 읽는 시점에 판단하므로 파기 배치가 아직 안 돌았어도 열람은 즉시 막힌다.
     */
    public static boolean isExpired(OffsetDateTime expiresAt) {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    /**
     * 보관함 상세(폴링 대응). status 봉투로 반환:
     *  - DONE       → result(본문 JSON) 채움
     *  - GENERATING → result/error 없음 (프론트: 작성 중… + 3초 폴링)
     *  - FAILED     → error 채움 (프론트: 재시도 버튼)
     *  - REVOKED    → 환불된 결제건 → 본문을 내려주지 않음
     * 본인 것 · 삭제되지 않은 것만.
     */
    public ResultDetailResponse getMine(Long id, Long memberId) {
        FortuneResult result = resultMapper.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "결과를 찾을 수 없습니다."));
        // 환불된 결제의 리포트는 상태와 무관하게 본문을 내려주지 않는다.
        // (읽는 시점에 판단하므로 웹훅이 늦게 와도 자동으로 맞아떨어진다)
        if (isRevoked(result.getPaymentStatus())) {
            return new ResultDetailResponse(id, "REVOKED", null,
                    "환불이 완료된 리포트예요. 다시 보시려면 새로 신청해 주세요.", false);
        }
        // 제공 기간이 끝난 건도 마찬가지로 본문을 내려주지 않는다.
        // 배치가 이미 본문을 비웠을 수도 있으므로 result_json 을 읽기 전에 먼저 걸러야 한다.
        if (isExpired(result.getExpiresAt())) {
            return new ResultDetailResponse(id, "EXPIRED", null,
                    "제공 기간(결제일로부터 1년)이 지나 열람이 종료된 리포트예요.", false);
        }
        String status = result.getStatus();
        if ("DONE".equals(status)) {
            JsonNode tree;
            try {
                tree = JSON.readTree(result.getResultJson());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "저장된 결과를 읽을 수 없습니다.");
            }
            return new ResultDetailResponse(id, "DONE", tree, null, false, inputOf(result),
                    ratingMapper.findByResultId(id));
        }
        if ("FAILED".equals(status)) {
            // 시도 한도에 도달했으면 재시도 버튼을 숨긴다(이미 자동 환불된 건).
            boolean retryable = result.getAttempts() < FortuneGenerationService.MAX_ATTEMPTS
                    && result.getInputJson() != null;
            return new ResultDetailResponse(id, "FAILED", null, result.getError(), retryable);
        }
        return new ResultDetailResponse(id, "GENERATING", null, null, false);
    }

    /**
     * 저장해 둔 생성 입력에서 GenerateRequest 부분만 꺼낸다.
     * input_json 형태: {"input": GenerateRequest, "intro": "..."}.
     * 없거나 깨져 있으면 null (프론트는 이 경우 입력 정보 표시를 생략한다).
     */
    private JsonNode inputOf(FortuneResult result) {
        String raw = result.getInputJson();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(raw).get("input");
            return node != null && !node.isNull() ? node : null;
        } catch (Exception e) {
            return null;   // 표시용 부가 정보라 실패해도 리포트 조회는 계속돼야 한다.
        }
    }

    /**
     * 보관함에서 리포트 지우기(소프트 삭제). 본인 것만.
     *
     * <p>행을 실제로 지우지 않는 이유:
     * <ul>
     *   <li>하드 삭제하면 '결제했는데 리포트가 없는 건'으로 잡혀 미수령 결제 배너가 되살아난다.</li>
     *   <li>payment_id 로 걸린 멱등 판정이 풀려 결제 없이 리포트를 다시 생성할 수 있게 된다.</li>
     * </ul>
     * 결제 이력(payments)은 어떤 경우에도 건드리지 않는다.
     */
    @Transactional
    public void deleteMine(Long id, Long memberId) {
        FortuneResult row = resultMapper.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "결과를 찾을 수 없습니다."));
        if ("GENERATING".equals(row.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "아직 작성 중인 리포트예요. 완료된 뒤에 삭제해 주세요.");
        }
        if (resultMapper.softDeleteByIdAndMemberId(id, memberId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "결과를 찾을 수 없습니다.");
        }
    }
}
