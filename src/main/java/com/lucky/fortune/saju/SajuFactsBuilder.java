package com.lucky.fortune.saju;

import com.lucky.fortune.dto.Subject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Subject 목록 → AI에게 넘길 결정적 '사실(facts)' JSON.
 *
 * <p>예전에는 일주 한 줄과 오행 다섯 숫자만 넘겼다. 그 오행조차 8글자가 아니라
 * 일간·일지·계절로 계산한 값이라, 오행 비중이 비슷한 두 사주를 AI가 구분할 수 없었다.
 * 지금은 <b>팔자 8글자와 각 글자의 십신·음양, 월령, 신강약</b>까지 넘긴다.
 * 전부 계산으로 나오는 값이라 같은 사주면 언제나 같다.
 *
 * <pre>
 * { "subjects":[{ "label","name","ilju","palja":{...},"chars":[{position,korean,hanja,element,yinYang,sipsin}..],
 *                 "ilgan","ilganElement","ilganYinYang","wolryeong","orbs":[{el,v}..],
 *                 "yinYang":{양,음}, "strength":{verdict,score,basis} }..],
 *   "pair":{"syncRate","temperature","distancePos"} }   // 2명일 때만
 * </pre>
 */
@Component
public class SajuFactsBuilder {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SajuCalculator calc;
    private final SajuAnalyzer analyzer;

    public SajuFactsBuilder(SajuCalculator calc, SajuAnalyzer analyzer) {
        this.calc = calc;
        this.analyzer = analyzer;
    }

    public JsonNode build(List<Subject> subjects) {
        ObjectNode facts = JSON.createObjectNode();
        ArrayNode arr = facts.putArray("subjects");
        List<SajuCalculator.Pillar> pillars = new ArrayList<>();

        for (Subject s : subjects) {
            SajuCalculator.Pillar p = calc.pillar(s);
            pillars.add(p);

            SajuCalculator.Palja palja = calc.palja(s);
            SajuAnalyzer.Profile profile = analyzer.analyze(palja);

            ObjectNode so = arr.addObject();
            so.put("label", s.label());
            so.put("name", s.name());
            so.put("ilju", calc.iljuLabel(s, p));

            ObjectNode pal = so.putObject("palja");
            pal.put("년주", palja.year().cheongan() + palja.year().jiji());
            pal.put("월주", palja.month().cheongan() + palja.month().jiji());
            pal.put("일주", palja.day().cheongan() + palja.day().jiji());
            pal.put("시주", palja.hour().cheongan() + palja.hour().jiji());
            if (s.timeUnknown()) {
                pal.put("비고", "출생 시각을 몰라 시주는 계산하지 않았다. 시주가 필요한 해석은 하지 말 것.");
            }

            ArrayNode chars = so.putArray("chars");
            for (SajuAnalyzer.Gan g : profile.chars()) {
                ObjectNode c = chars.addObject();
                c.put("자리", g.position());
                c.put("글자", g.korean());
                c.put("한자", g.hanja());
                c.put("오행", g.element());
                c.put("음양", g.yinYang());
                c.put("십신", g.sipsin());
            }

            so.put("일간", profile.ilgan());
            so.put("일간오행", profile.ilganElement());
            so.put("일간음양", profile.ilganYinYang());
            so.put("월령", profile.wolryeong());
            so.put("월령오행", profile.wolryeongElement());

            ArrayNode orbs = so.putArray("orbs");
            for (Orb o : profile.orbs()) {
                ObjectNode on = orbs.addObject();
                on.put("el", o.el());
                on.put("v", o.v());
            }

            ObjectNode yy = so.putObject("음양");
            for (Map.Entry<String, Integer> e : profile.yinYangCount().entrySet()) {
                yy.put(e.getKey(), e.getValue());
            }

            ObjectNode st = so.putObject("신강약");
            st.put("판정", profile.strength().verdict());
            st.put("점수", profile.strength().score());
            st.put("근거", profile.strength().basis());
            st.put("주의", "판정법은 학파마다 다르다. 단정하지 말고 근거와 함께 완곡히 다뤄라.");
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
