package com.lucky.saju.dto;

import java.util.List;

/** 사주 풀이 결과 (프론트 UI 구조에 맞춘 JSON). */
public record SajuResponse(
        Ilju ilju,
        Palja palja,
        List<Fortune> fortunes,
        String summary
) {

    /** 일주 정보: 이름(예: 갑자일주), 한자(예: 甲子), 감각적 한 줄 묘사. */
    public record Ilju(String name, String hanja, String description) {
    }

    /** 사주팔자 8글자. 시(hour)/일(day)/월(month)/년(year) 기둥. */
    public record Palja(Pillar hour, Pillar day, Pillar month, Pillar year) {
    }

    /** 한 기둥: 천간(위)/지지(아래). 각 한자 한 글자. */
    public record Pillar(String cheongan, String jiji) {
    }

    /** 분야별 운세: 카테고리, 점수(0~100), 한 줄 코멘트. */
    public record Fortune(String category, int score, String comment) {
    }
}
