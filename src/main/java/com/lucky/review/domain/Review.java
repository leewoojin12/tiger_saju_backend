package com.lucky.review.domain;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 이용 후기(reviews 테이블). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    private Long id;
    /** 작성자 회원. 이관·운영자 등록 건은 NULL. */
    private Long memberId;
    /** 어떤 리포트에 대한 후기인지. 리포트 1건당 1개(부분 UNIQUE). 이관 건은 NULL. */
    private Long resultId;
    private String slug;
    /** 작성 시점의 상품명 스냅샷. slug 이 바뀌어도 후기 표시는 흔들리지 않는다. */
    private String product;
    /** 화면에 찍히는 이름(마스킹된 형태로 저장). */
    private String author;
    /** 1~5 */
    private int rating;
    private String body;
    /** PENDING / PUBLIC / HIDDEN */
    private String status;
    /** 화면에 표시하는 작성일(이관 건은 원본 날짜). */
    private OffsetDateTime writtenAt;
    private OffsetDateTime createdAt;
}
