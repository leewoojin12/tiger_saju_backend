package com.lucky.member.domain;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    private Long id;
    private Long kakaoId;
    private String nickname;
    private String role;          // 예: ROLE_USER
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
