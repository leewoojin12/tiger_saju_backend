package com.lucky.payment.client;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** PortOne V2 REST API 호출 래퍼. */
@Component
@RequiredArgsConstructor
public class PortOneApiClient {

    private final RestClient portOneRestClient;

    /** 단건 결제 조회. 검증용 파싱 객체 반환. (paymentId 는 RestClient 가 URL 인코딩) */
    public PortOnePayment getPayment(String paymentId) {
        return portOneRestClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .body(PortOnePayment.class);
    }

    /** 단건 결제 조회 원본 JSON (감사/저장용). */
    public String getPaymentRaw(String paymentId) {
        return portOneRestClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .retrieve()
                .body(String.class);
    }

    /** 결제 취소(환불). 운영에선 관리자 권한으로만 노출하세요. */
    public void cancel(String paymentId, String reason) {
        portOneRestClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reason", reason))
                .retrieve()
                .toBodilessEntity();
    }
}
