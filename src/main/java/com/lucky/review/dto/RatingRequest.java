package com.lucky.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 리포트 별점 남기기 요청. 글 없이 점수만 받는다.
 *
 * @param rating 1~5. 범위를 벗어나면 400 (DB 의 CHECK 제약과 같은 범위).
 */
public record RatingRequest(@Min(1) @Max(5) int rating) {}
