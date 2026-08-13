package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 라벨별 건수 (결제상태/결제수단/역할/성별/달력/시간모름/콘텐츠 생성수 등 분포 공통). */
@Getter
@Setter
@NoArgsConstructor
public class LabelCount {
    private String label;
    private long count;
}
