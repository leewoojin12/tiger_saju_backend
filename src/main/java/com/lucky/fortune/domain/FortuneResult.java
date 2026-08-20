package com.lucky.fortune.domain;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 결제 완료 후 생성된 풀 리포트 결과(fortune_results 테이블). 결제 1건당 1행. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FortuneResult {

    private Long id;
    private Long memberId;
    private String slug;
    private String paymentId;
    private String name;
    private String resultJson;   // 풀 리포트 AI JSON(원문). 생성 중(GENERATING)이면 NULL.
    private String status;       // GENERATING / DONE / FAILED
    private String error;        // FAILED 시 사용자 안내 메시지
    private String inputJson;    // 재시도용 생성 입력 {"input":..,"intro":..}
    private int attempts;        // 생성 시도 횟수(최초 1회 + 재시도). 한도 도달 시 자동 환불.
    private OffsetDateTime failedAt;   // 마지막 실패 시각
    private OffsetDateTime startedAt;  // 마지막으로 GENERATING 이 된 시각(좀비 행 판정 기준)
    private OffsetDateTime deletedAt;  // 보관함에서 사용자가 지운 시각(소프트 삭제). NULL 이면 살아있음.
    private OffsetDateTime createdAt;

    /**
     * 조인 투영 전용: 이 리포트에 연결된 결제건의 상태(payments.status).
     * 테이블 컬럼이 아니라 findByIdAndMemberId 조회 시 LEFT JOIN 으로 채워진다.
     * 환불(CANCELLED/PARTIAL_CANCELLED)된 리포트의 열람 차단 판단에 쓴다.
     */
    private String paymentStatus;
}
