package com.lucky.payment.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 웹훅 본문(필요한 필드만).
 * type 예: Transaction.Ready / Transaction.Paid / Transaction.Cancelled / Transaction.VirtualAccountIssued ...
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(String type, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String paymentId, String cancellationId, String transactionId) {
    }
}
