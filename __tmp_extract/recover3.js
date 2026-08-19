const fs = require("fs");

// 1) prepare 요청에 입력 스냅샷(선택) 추가
const rp = "src/main/java/com/lucky/payment/dto/PreparePaymentRequest.java";
fs.writeFileSync(rp, `package com.lucky.payment.dto;

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
`);

// 2) 미수령 결제 응답 DTO
fs.writeFileSync("src/main/java/com/lucky/payment/dto/UnfulfilledPayment.java", `package com.lucky.payment.dto;

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
`);
console.log("dto ok");
