package com.lucky.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yaml 의 portone.* 설정 바인딩. */
@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(
        String apiSecret,
        String webhookSecret,
        String baseUrl,
        String storeId
) {
}
