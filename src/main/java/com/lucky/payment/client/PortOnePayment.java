package com.lucky.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;

/**
 * PortOne V2 단건 결제 조회 응답(필요한 필드만).
 * status: READY | PAID | FAILED | CANCELLED | PARTIAL_CANCELLED | VIRTUAL_ACCOUNT_ISSUED | PAY_PENDING
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePayment(
        String id,
        String status,
        String orderName,
        String currency,
        Amount amount,
        OffsetDateTime paidAt
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(long total, long paid, long cancelled) {
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public long totalAmount() {
        return amount != null ? amount.total() : 0L;
    }
}
