package com.lucky.fortune.service;

import com.lucky.saju.dto.SajuRequest;

/**
 * 사용자 입력(SajuRequest)을 AI '유저 메시지'로 변환.
 * 운세별 프롬프트는 system 메시지로, 이 입력은 user 메시지로 분리해서 보낸다(프롬프트 주입 방지).
 */
public final class SajuInputFormatter {

    private SajuInputFormatter() {
    }

    public static String toUserMessage(SajuRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("이름: ").append(req.name()).append('\n');
        sb.append("성별: ").append(req.gender()).append('\n');
        sb.append("달력: ").append(req.calendar()).append('\n');
        sb.append("생년월일: ").append(req.birthDate()).append('\n');
        if (req.timeUnknown()) {
            sb.append("태어난 시간: 모름\n");
        } else {
            sb.append("태어난 시간: ").append(req.birthTime() == null ? "미입력" : req.birthTime()).append('\n');
        }
        sb.append("\n위 정보를 바탕으로, 지정된 JSON 형식으로만 응답해줘.");
        return sb.toString();
    }
}
