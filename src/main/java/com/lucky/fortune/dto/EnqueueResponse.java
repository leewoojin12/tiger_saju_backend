package com.lucky.fortune.dto;

/**
 * POST /full(생성 요청)·POST /retry(재시도) 즉시 응답.
 * 프론트는 이 id로 보관함 상세를 3초 폴링하며 status 가 DONE/FAILED 될 때까지 기다린다.
 */
public record EnqueueResponse(Long id, String status) {
}
