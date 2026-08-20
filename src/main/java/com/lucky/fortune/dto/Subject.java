package com.lucky.fortune.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 생성 입력에 들어가는 '한 사람'. 백엔드가 사주 계산에 쓰는 구조화 필드.
 *  - label    : "me" | "partner" 등 (프론트가 의미 부여, 백엔드는 그대로 보존)
 *  - calendar : "양력" | "음력"
 *  - leapMonth: 음력 윤달 여부 (양력이면 무시)
 *  - birthDate: "yyyy-MM-dd". 음력이면 음력 연/월/일.
 *  - birthTime/timeUnknown: v1 계산엔 미사용(시주 미구현), AI 컨텍스트로만 전달.
 *  - gender   : "남성" | "여성" (선택). 계산엔 미사용, AI 컨텍스트로 전달.
 *               궁합처럼 두 사람을 볼 때 성별이 빠지면 해석이 어긋나서 넣었다.
 *               예전에 저장된 입력(input_json)에는 없으므로 null 을 허용한다.
 */
public record Subject(
        String label,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "생년월일은 yyyy-MM-dd 형식")
        String birthDate,
        @NotBlank String calendar,
        boolean leapMonth,
        String birthTime,
        boolean timeUnknown,
        String gender
) {
    /** gender 도입 이전 호출부(테스트·팔자판 등) 호환용. */
    public Subject(String label, String name, String birthDate, String calendar,
                   boolean leapMonth, String birthTime, boolean timeUnknown) {
        this(label, name, birthDate, calendar, leapMonth, birthTime, timeUnknown, null);
    }
}
