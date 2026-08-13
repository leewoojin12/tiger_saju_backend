package com.lucky.saju.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 보관함 상세 1건: 입력 정보 + 전체 사주 풀이 결과. */
public record SajuHistoryDetail(
        Long id,
        String name,
        String gender,
        String calendar,
        LocalDate birthDate,
        String birthTime,
        boolean timeUnknown,
        OffsetDateTime createdAt,
        SajuResponse result
) {
}
