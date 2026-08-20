package com.lucky.fortune.dto;

import com.lucky.fortune.domain.Fortune;
import java.time.OffsetDateTime;

/**
 * 공개 카탈로그 응답 DTO. 프롬프트(teaser/full)는 절대 포함하지 않는다.
 * 프론트 랜딩 페이지가 가격/제목/문구를 여기서 받아 표시 → 하드코딩 없이 항상 DB와 동기화.
 * category/createdAt 은 메인 탭 필터·NEW 뱃지(7일 이내)용.
 */
public record FortunePublic(
        String slug,
        String title,
        String description,
        String durationText,
        long price,
        String category,
        /** 노출 순서(낮을수록 앞). 비어 있으면 null — 목록은 이미 이 순서로 정렬돼 온다. */
        Integer sortOrder,
        OffsetDateTime createdAt,
        String uiConfig
) {
    public static FortunePublic from(Fortune f) {
        return new FortunePublic(
                f.getSlug(),
                f.getTitle(),
                f.getDescription(),
                f.getDurationText(),
                f.getPrice(),
                f.getCategory(),
                f.getSortOrder(),
                f.getCreatedAt(),
                f.getUiConfig()
        );
    }
}
