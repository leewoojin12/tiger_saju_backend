package com.lucky.saju.dto;

/**
 * 팔자판 전용 경량 응답(AI·보관함 저장 없음).
 * 천간/지지는 한자 한 글자. 시간 모르면 hour 두 값이 빈 문자열.
 * 프론트 팔자판 UI 구조(ilju.name + palja 4기둥)에 맞춘다.
 */
public record PaljaResponse(
        Ilju ilju,
        Palja palja
) {
    public record Ilju(String name) {
    }

    public record Palja(Pillar hour, Pillar day, Pillar month, Pillar year) {
    }

    public record Pillar(String cheongan, String jiji) {
    }
}
