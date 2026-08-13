package com.lucky.saju.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사주 풀이 결과 보관함 레코드 (saju_history 테이블). */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SajuHistory {

    private Long id;
    private Long memberId;        // 소유자(로그인 회원)
    private String username;      // 표시용(저장 시점 닉네임)
    private String name;          // 사주 대상 이름 (SajuRequest.name)
    private String gender;
    private String calendar;
    private LocalDate birthDate;
    private String birthTime;
    private boolean timeUnknown;
    private String iljuName;      // 목록 미리보기
    private String summary;       // 목록 미리보기
    private String resultJson;    // SajuResponse 전체 JSON
    private OffsetDateTime createdAt;
}
