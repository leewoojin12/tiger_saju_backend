package com.lucky.saju.client;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OpenAiClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final int MAX_RETRIES = 3;

    private final RestClient openAiRestClient;
    private final String model;
    private final int maxCompletionTokens;

    public OpenAiClient(RestClient openAiRestClient,
                        @Value("${openai.model}") String model,
                        @Value("${openai.max-completion-tokens:1200}") int maxCompletionTokens) {
        this.openAiRestClient = openAiRestClient;
        this.model = model;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, maxCompletionTokens);
    }

    @Override
    public String complete(String systemPrompt, String userPrompt, int maxCompletionTokens) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_completion_tokens", maxCompletionTokens,
                "response_format", Map.of("type", "json_object")
        );

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                JsonNode response = openAiRestClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                return extractContent(response);
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("OpenAI 429 (attempt {})", attempt); // 키/본문은 로깅하지 않는다
                if (attempt > MAX_RETRIES) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "AI 요청이 잠시 제한되었습니다. 잠시 후 다시 시도해 주세요.");
                }
                backoff(attempt, retryAfterMillis(e.getResponseHeaders()));
            } catch (HttpServerErrorException e) {
                log.warn("OpenAI 5xx {} (attempt {})", e.getStatusCode().value(), attempt);
                if (attempt > MAX_RETRIES) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "AI 서버 오류로 응답을 받지 못했습니다.");
                }
                backoff(attempt, 0L);
            } catch (HttpClientErrorException e) {
                // 4xx(잘못된 모델명, 인증 실패 등): 재시도해도 동일하므로 즉시 중단. 키는 로깅 금지.
                log.error("OpenAI 4xx {}", e.getStatusCode().value());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 호출이 거부되었습니다(모델명/키 설정 확인 필요).");
            } catch (RestClientException e) {
                // 타임아웃·네트워크 등
                log.error("OpenAI 호출 실패: {}", e.getClass().getSimpleName());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 호출에 실패했습니다.");
            }
        }
    }

    private String extractContent(JsonNode response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답이 비어 있습니다.");
        }
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답을 해석할 수 없습니다.");
        }
        return content.asText();
    }

    private long retryAfterMillis(HttpHeaders headers) {
        if (headers == null) {
            return 0L;
        }
        String retryAfter = headers.getFirst("Retry-After");
        if (retryAfter == null) {
            return 0L;
        }
        try {
            return Long.parseLong(retryAfter.trim()) * 1000L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void backoff(int attempt, long retryAfterMillis) {
        long base = (long) (500 * Math.pow(2, attempt - 1)); // 500, 1000, 2000ms ...
        long wait = Math.max(base, retryAfterMillis);
        try {
            Thread.sleep(wait);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "요청이 중단되었습니다.");
        }
    }
}
