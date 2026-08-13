package com.lucky.fortune.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 풀 리포트 생성 요청.
 *  - paymentId : 완료된 결제건 ID (prepare→requestPayment→complete 거친 그 id)
 *  - intro     : 맛보기에서 보여준 도입부(프론트가 운반). 풀 본문이 여기 '이어서' 작성됨. (없어도 됨)
 *  - input     : 생성 입력(subjects/answers). facts는 서버가 재계산하므로 입력은 동일해야 함.
 */
public record FullReportRequest(
        @NotBlank String paymentId,
        String intro,
        @Valid @NotNull GenerateRequest input
) {
}
