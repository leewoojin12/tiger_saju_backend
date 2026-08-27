package com.lucky.share.dto;

import java.time.OffsetDateTime;

/**
 * 공유 링크 발급 결과.
 *
 * @param token     주소에 붙일 토큰. 서버는 해시만 보관하므로 이 순간이 지나면 다시 알 수 없다.
 * @param expiresAt 이 링크가 죽는 시각
 */
public record ShareLinkResponse(String token, OffsetDateTime expiresAt) {}
