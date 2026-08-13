package com.lucky.payment.dto;

/** 결제 준비 응답. 프론트는 이 paymentId 로 PortOne.requestPayment 를 호출한다. */
public record PreparePaymentResponse(
        String paymentId,
        String orderName,
        long amount,
        String currency,
        String storeId   // 공개값(프론트 env 와 동일). 편의상 함께 반환
) {
}
