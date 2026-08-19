package com.lucky.payment.dto;

import java.time.OffsetDateTime;

/**
 * 결제는 됐는데 리포트가 만들어지지 않은 결제건.
 * 보관함에서 "받지 못한 리포트 이어받기" 안내에 사용한다.
 */
public record UnfulfilledPayment(
        String paymentId,
        String slug,
        String orderName,
        long amount,
        OffsetDateTime paidAt,
        boolean canResume    // 저장된 입력이 있어 바로 생성 가능한지
) {
}
