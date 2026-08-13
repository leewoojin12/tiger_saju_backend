package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** payments 전체 요약 행 (매출 합/PAID 수/전체 결제행 수). */
@Getter
@Setter
@NoArgsConstructor
public class PaymentSummaryRow {
    private long totalRevenue;  // PAID amount 합
    private long paidCount;     // PAID 건수
    private long totalCount;    // 전체 결제행(PENDING 포함) — 성공률 분모
}
