package com.lucky.saju.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 사주 입력.
 * gender: "남성" | "여성", calendar: "양력" | "음력"
 * birthTime: 예) "자시 (23–01시)". timeUnknown=true 이면 무시.
 */
public record SajuRequest(
        @NotBlank String name,
        @NotBlank String gender,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate birthDate,
        String birthTime,
        boolean timeUnknown,
        @NotBlank String calendar
) {
}
