package com.lucky.share.dto;

import java.time.OffsetDateTime;

/**
 * 공유 토큰 조회 결과(report_shares + fortune_results + payments 조인 한 줄).
 * 열람 가능 여부를 판단하는 데 필요한 값만 담는다.
 *
 * @param shareExpiresAt  링크 만료 시각
 * @param revokedAt       링크를 끊은 시각(NULL 이면 살아있음)
 * @param resultExpiresAt 콘텐츠 제공 기간 종료 시각(결제일 + 1년)
 * @param paymentStatus   결제 상태. 환불(CANCELLED 등)이면 본문을 내려주지 않는다
 */
public record SharedRow(
        OffsetDateTime shareExpiresAt,
        OffsetDateTime revokedAt,
        String slug,
        String resultJson,
        String inputJson,
        String status,
        OffsetDateTime deletedAt,
        OffsetDateTime resultExpiresAt,
        String paymentStatus
) {}
