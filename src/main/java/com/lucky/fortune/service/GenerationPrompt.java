package com.lucky.fortune.service;

import com.lucky.fortune.dto.GenerateRequest;
import com.lucky.fortune.dto.Subject;
import tools.jackson.databind.JsonNode;

/**
 * 생성 user 메시지 빌더. 시스템 메시지는 DB의 프롬프트(teaser/full)가 맡고,
 * 여기서는 입력(subjects/answers)과 '계산된 사실(facts)'을 전달한다.
 * facts의 숫자/일주는 결정적 계산값이므로 "바꾸지 말고 해석만" 하도록 못 박는다.
 */
final class GenerationPrompt {

    private GenerationPrompt() {
    }

    static String user(GenerateRequest req, JsonNode facts, String intro) {
        StringBuilder sb = new StringBuilder();
        sb.append("[대상]\n");
        for (Subject s : req.subjects()) {
            sb.append("- ");
            if (s.label() != null && !s.label().isBlank()) {
                sb.append(s.label()).append(' ');
            }
            sb.append(s.name()).append(": ").append(s.birthDate()).append(' ').append(s.calendar());
            if (s.timeUnknown()) {
                sb.append(" 시간모름");
            } else if (s.birthTime() != null && !s.birthTime().isBlank()) {
                sb.append(' ').append(s.birthTime());
            }
            sb.append('\n');
        }

        if (req.answers() != null && !req.answers().isEmpty()) {
            sb.append("\n[추가 입력]\n");
            req.answers().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append('\n'));
        }

        sb.append("\n[계산된 사실 — 아래 값은 이미 정확히 계산되었다. 숫자·일주를 절대 바꾸지 말고 이 값을 근거로 해석만 하라]\n");
        sb.append(facts.toString()).append('\n');

        if (intro != null && !intro.isBlank()) {
            sb.append("\n[도입부 — 다음 도입부에 자연스럽게 '이어서' 작성하라. 도입부 문장을 반복하지 말 것]\n");
            sb.append(intro).append('\n');
        }

        sb.append("\n반드시 지정된 JSON 형식으로만 응답하라(설명·코드블록 금지).");
        return sb.toString();
    }
}
