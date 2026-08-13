package com.lucky.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

    @Bean
    public RestClient openAiRestClient(
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.api-key}") String apiKey,
            @Value("${openai.timeout-seconds:60}") long timeoutSeconds) {

        // 타임아웃 설정 (llm-integration.md 규칙)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        // Authorization 헤더에 키를 기본 세팅 (요청 시마다 노출/로깅하지 않도록 한 곳에서만 처리)
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(factory)
                .build();
    }
}
