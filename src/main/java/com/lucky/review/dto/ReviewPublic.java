package com.lucky.review.dto;

import java.time.OffsetDateTime;

/**
 * 공개용 후기 한 건. 회원 id·리포트 id 같은 내부 식별자는 절대 싣지 않는다.
 *
 * @param author    화면에 찍히는 이름(마스킹된 형태)
 * @param product   후기를 쓴 시점의 상품명
 * @param rating    1~5
 * @param writtenAt 표시용 작성일
 */
public record ReviewPublic(
        Long id,
        String slug,
        String product,
        String author,
        int rating,
        String body,
        OffsetDateTime writtenAt
) {}
