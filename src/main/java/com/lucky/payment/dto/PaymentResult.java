package com.lucky.payment.dto;

/** 검증 결과 응답. */
public record PaymentResult(
        String paymentId,
        String status,   // PAID / READY / FAILED / ...
        long amount,
        boolean paid
) {
}
