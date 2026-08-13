package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 카테고리별 매출(PAID) + 생성수(fortune_results). */
@Getter
@Setter
@NoArgsConstructor
public class CategoryStat {
    private String category;
    private long revenue;
    private long genCount;
}
