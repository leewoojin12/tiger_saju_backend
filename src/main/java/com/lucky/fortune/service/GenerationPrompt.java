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
            if (s.gender() != null && !s.gender().isBlank()) {
                sb.append(' ').append(s.gender());
            }
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

        // 미입력 항목을 명시 → AI 가 "질문하신 대로" 처럼 없는 입력을 있다고 쓰지 않도록.
        boolean hasQuestion = req.answers() != null
                && req.answers().keySet().stream().anyMatch(k -> k.toLowerCase().contains("question"));
        if (!hasQuestion) {
            sb.append("\n[미입력] 이번 의뢰인은 개별 질문을 남기지 않았다.")
              .append(" '질문하신', '문의하신' 같은 표현을 쓰지 말고, 질문이 있었던 것처럼 답하지 말 것.")
              .append(" 질문 답변에 해당하는 장은 이 사주에서 가장 도움이 될 조언으로 대신하라.\n");
        }

        // 호칭 규칙 → "사용자님" 같은 일반 호칭 대신 실제 이름을 쓰게 한다.
        String mainName = req.subjects().isEmpty() ? null : req.subjects().get(0).name();
        if (mainName != null && !mainName.isBlank()) {
            sb.append("\n[호칭] 의뢰인을 부를 때는 반드시 \"").append(mainName).append("님\" 으로 쓴다.")
              .append(" '사용자님', '고객님', '의뢰인님' 같은 표현은 사용하지 말 것.\n");
        }

        sb.append("\n[계산된 사실 — 아래 값은 이미 정확히 계산되었다. 숫자·글자를 절대 바꾸지 말고 이 값을 근거로 해석만 하라]\n");
        sb.append(facts.toString()).append('\n');
        sb.append("[사실 활용 지침]\n")
          .append("- chars 의 십신은 오행뿐 아니라 음양까지 따진 값이다. 같은 오행이어도 갑목과 을목,")
          .append(" 신금과 경금은 십신이 달라진다. 오행만 뭉뚱그리지 말고 글자 하나하나를 근거로 써라.\n")
          .append("- 월령(월지)은 계절의 지배력이다. 원국에 약한 기운도 월령이 받쳐주면 달리 본다.\n")
          .append("- 신강약은 판정법이 학파마다 다르다. 단정적으로 규정하지 말고 근거와 함께 완곡히 다뤄라.\n")
          .append("- 시주가 없으면(출생 시각 미상) 시주에 기대는 해석은 아예 하지 말 것.\n");

        if (intro != null && !intro.isBlank()) {
            sb.append("\n[도입부 — 다음 도입부에 자연스럽게 '이어서' 작성하라. 도입부 문장을 반복하지 말 것]\n");
            sb.append(intro).append('\n');
        }

        sb.append("\n반드시 지정된 JSON 형식으로만 응답하라(설명·코드블록 금지).");
        return sb.toString();
    }
}
