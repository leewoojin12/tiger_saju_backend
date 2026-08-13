package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 콘텐츠(product_code)별 판매 집계. title 은 fortunes 조인(없으면 null). PAID 기준. */
@Getter
@Setter
@NoArgsConstructor
public class ContentSale {
    private String slug;     // payments.product_code
    private String title;    // fortunes.title (없으면 null)
    private long revenue;    // PAID amount 합
    private long count;      // PAID 건수
}
