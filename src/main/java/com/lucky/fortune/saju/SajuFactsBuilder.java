package com.lucky.fortune.saju;

import com.lucky.fortune.dto.Subject;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Subject 목록 → 결정적 '사실(facts)' JSON.
 * 각 사람마다 일주(ilju)+오행(orbs), 정확히 2명이면 궁합 수치(pair) 추가.
 * 컨텐츠 종류를 모르고 제네릭하게 계산 → 프론트가 subjects[0]/[1]을 me/partner로 매핑.
 *
 * <pre>
 * { "subjects":[{"label","name","ilju","orbs":[{el,v}..]}..],
 *   "pair":{"syncRate","temperature","distancePos"} }   // 2명일 때만
 * </pre>
 */
@Component
public class SajuFactsBuilder {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SajuCalculator calc;

    public SajuFactsBuilder(SajuCalculator calc) {
        this.calc = calc;
    }

    public JsonNode build(List<Subject> subjects) {
        ObjectNode facts = JSON.createObjectNode();
        ArrayNode arr = facts.putArray("subjects");
        List<SajuCalculator.Pillar> pillars = new ArrayList<>();

        for (Subject s : subjects) {
            SajuCalculator.Pillar p = calc.pillar(s);
            pillars.add(p);
            ObjectNode so = arr.addObject();
            so.put("label", s.label());
            so.put("name", s.name());
            so.put("ilju", calc.iljuLabel(s, p));
            ArrayNode orbs = so.putArray("orbs");
            for (Orb o : calc.orbs(p)) {
                ObjectNode on = orbs.addObject();
                on.put("el", o.el());
                on.put("v", o.v());
            }
        }

        if (pillars.size() == 2) {
            ObjectNode pair = facts.putObject("pair");
            pair.put("syncRate", calc.syncRate(pillars.get(0), pillars.get(1)));
            pair.put("temperature", calc.temperature(pillars.get(0), pillars.get(1)));
            pair.put("distancePos", calc.distancePos(pillars.get(0), pillars.get(1)));
        }
        return facts;
    }
}
