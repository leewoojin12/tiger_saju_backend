package com.lucky.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/** PortOne V2 REST API 호출용 RestClient. 모든 요청에 Authorization 헤더 자동 부착. */
@Configuration
@EnableConfigurationProperties(PortOneProperties.class)
public class PortOneConfig {

    @Bean
    public RestClient portOneRestClient(PortOneProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                // V2 인증: "PortOne {API_SECRET}" 형식
                .defaultHeader(HttpHeaders.AUTHORIZATION, "PortOne " + props.apiSecret())
                .build();
    }
}
