package com.lucky.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 결제 준비 요청. 프론트는 <b>slug만</b> 보낸다.
 * 가격·상품명은 서버가 fortunes 테이블(slug)에서 결정한다 → 클라이언트가 금액을 위조할 수 없음.
 */
public record PreparePaymentRequest(
        @NotBlank String slug
) {
}
