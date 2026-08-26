package com.lucky.review.dto;

/**
 * 별점 저장 결과. 이미 남긴 리포트면 '처음 남긴 그 점수'가 그대로 돌아온다
 * (덮어쓰지 않으므로 요청한 값과 다를 수 있다). 화면은 이 값으로 잠근다.
 */
public record RatingResponse(int rating) {}
