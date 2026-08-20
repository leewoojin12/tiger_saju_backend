package com.lucky.fortune.dto;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보관함 목록 항목. fortune_results + fortunes(title) 조인 투영.
 * 본문(result_json)은 목록에서 제외(상세에서만).
 */
@Getter
@Setter
@NoArgsConstructor
public class MyResultSummary {

    private Long id;
    private String slug;
    private String title;   // fortunes.title (없으면 null)
    private String name;    // 대상 이름(첫 번째 subject)
    private String status;  // GENERATING / DONE / FAILED (목록 뱃지용)
    /** 연결된 결제건 상태(payments.status). CANCELLED/PARTIAL_CANCELLED 면 환불된 리포트. */
    private String paymentStatus;
    private OffsetDateTime createdAt;
}
