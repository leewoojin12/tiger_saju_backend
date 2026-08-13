package com.lucky.saju.dto;

import java.time.OffsetDateTime;

/** 보관함 목록 1건(가벼운 미리보기). 전체 결과는 상세 API에서. */
public record SajuHistoryListItem(
        Long id,
        String name,
        String iljuName,
        String summary,
        OffsetDateTime createdAt
) {
}
