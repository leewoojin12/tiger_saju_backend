package com.lucky.payment.dto;

import jakarta.validation.constraints.NotBlank;

/** 결제 완료 검증 요청. 프론트 requestPayment 성공 후 paymentId 를 보낸다. */
public record CompletePaymentRequest(
        @NotBlank String paymentId
) {
}
