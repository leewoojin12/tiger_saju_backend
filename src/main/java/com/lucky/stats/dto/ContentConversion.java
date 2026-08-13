package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 콘텐츠별 결제 대비 생성완료 전환 (paid → DONE). pct 는 프론트 계산. */
@Getter
@Setter
@NoArgsConstructor
public class ContentConversion {
    private String slug;
    private String title;
    private long paidCount;
    private long doneCount;
}
