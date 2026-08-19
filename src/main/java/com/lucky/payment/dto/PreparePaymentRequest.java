package com.lucky.payment.dto;

import com.lucky.fortune.dto.GenerateRequest;
import jakarta.validation.constraints.NotBlank;

/**
 * 결제 준비 요청. 가격·상품명은 서버가 fortunes(slug)에서 결정한다 → 금액 위조 불가.
 *
 * <p>{@code input} 은 사용자가 입력한 사주 정보(선택). 결제 직후 브라우저가 닫혀
 * 생성 요청이 유실돼도 서버가 리포트를 만들어 줄 수 있도록 결제건에 함께 보관한다.
 */
public record PreparePaymentRequest(
        @NotBlank String slug,
        GenerateRequest input
) {
}
