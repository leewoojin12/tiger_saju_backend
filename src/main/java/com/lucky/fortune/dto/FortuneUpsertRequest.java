package com.lucky.fortune.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 관리자: 운세 생성/수정 입력.
 *  - 생성(POST): slug 포함 전체 사용.
 *  - 수정(PUT /{slug}): slug 는 경로로 식별하며 변경하지 않음(결제·결과 링크 보호). 나머지 필드만 반영.
 */
public record FortuneUpsertRequest(
        @NotBlank String slug,
        @NotBlank String title,
        String description,
        String durationText,
        @PositiveOrZero long price,
        boolean active,
        String category,
        String teaserPrompt,
        String fullPrompt,
        String uiConfig
) {
}
