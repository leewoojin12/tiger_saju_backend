package com.lucky.saju.client;

/** LLM 호출 추상화 (coding-style.md: 외부 연동은 인터페이스로). */
public interface LlmClient {

    /**
     * @param systemPrompt 시스템 역할/규칙
     * @param userPrompt   사용자 입력 기반 프롬프트
     * @return LLM이 생성한 텍스트
     */
    String complete(String systemPrompt, String userPrompt);

    /**
     * 출력 토큰 상한을 지정해 호출. 맛보기처럼 짧고 싸게 받아야 할 때 사용.
     *
     * @param maxCompletionTokens 출력 토큰 상한 (작을수록 저렴·빠름)
     */
    String complete(String systemPrompt, String userPrompt, int maxCompletionTokens);
}
