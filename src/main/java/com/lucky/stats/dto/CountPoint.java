package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 일별 건수 포인트 (가입 추이 등). */
@Getter
@Setter
@NoArgsConstructor
public class CountPoint {
    private String date;   // YYYY-MM-DD (Asia/Seoul)
    private long count;
}
