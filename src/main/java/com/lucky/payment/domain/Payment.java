package com.lucky.payment.domain;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 결제 내역(payments 테이블). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    private Long id;
    private String paymentId;     // PortOne 결제건 ID(서버 생성)
    private Long memberId;
    private String productCode;   // 결제 대상 컨텐츠 slug (fortunes.slug)
    private String orderName;
    private long amount;          // 기대 금액(원)
    private String currency;
    private String payMethod;
    private String status;        // PENDING/PAID/FAILED/CANCELLED/...
    private String pgTxId;
    private String rawJson;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
