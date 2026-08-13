package com.lucky.stats.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 일별 매출 포인트 (KST 기준 날짜, 매출/건수). */
@Getter
@Setter
@NoArgsConstructor
public class DailyPoint {
    private String date;    // YYYY-MM-DD (Asia/Seoul)
    private long revenue;   // 해당일 PAID amount 합
    private long count;     // 해당일 PAID 건수
}
