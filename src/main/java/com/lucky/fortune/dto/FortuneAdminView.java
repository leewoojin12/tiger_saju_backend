package com.lucky.fortune.dto;

import com.lucky.fortune.domain.Fortune;

/**
 * 관리자 화면용 운세 뷰. 공개 DTO와 달리 <b>프롬프트(teaser/full)를 포함</b>한다.
 * /api/admin/** (ROLE_ADMIN) 에서만 반환되므로 노출 위험 없음.
 */
public record FortuneAdminView(
        Long id,
        String slug,
        String title,
        String description,
        String durationText,
        long price,
        boolean active,
        String category,
        /** 노출 순서(낮을수록 앞). 비우면 null → 등록순. */
        Integer sortOrder,
        String teaserPrompt,
        String fullPrompt,
        String uiConfig
) {
    public static FortuneAdminView from(Fortune f) {
        return new FortuneAdminView(
                f.getId(), f.getSlug(), f.getTitle(), f.getDescription(), f.getDurationText(),
                f.getPrice(), f.isActive(), f.getCategory(), f.getSortOrder(),
                f.getTeaserPrompt(), f.getFullPrompt(), f.getUiConfig()
        );
    }
}
