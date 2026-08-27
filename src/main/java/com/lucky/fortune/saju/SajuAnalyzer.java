package com.lucky.fortune.saju;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 팔자 8글자에서 해석의 재료를 뽑는다.
 *
 * <p><b>왜 만들었나.</b> 이 전까지 AI에게 넘어가던 사주 정보는 일주 한 줄과 오행 다섯 숫자가
 * 전부였다. 그 오행조차 8글자가 아니라 일간·일지·계절 세 가지로 계산한 값이었다.
 * 그래서 신축일주와 경진일주처럼 오행 비중이 비슷한 사주는 AI가 구분할 근거 자체가 없었고,
 * 같은 결의 해석이 나올 수밖에 없었다.
 *
 * <p><b>무엇을 더 주는가.</b> 여기서 만드는 값은 전부 <b>결정적</b>이다 — 같은 사주면 항상 같다.
 * <ul>
 *   <li>8글자 전부(천간 4 · 지지 4)와 각 글자의 오행·음양</li>
 *   <li>글자마다의 <b>십신</b> — 일간과의 관계. 오행뿐 아니라 음양까지 따지므로
 *       갑목과 을목, 신금과 경금이 여기서 갈린다. 학파 차이가 없는 부분이다.</li>
 *   <li>월령(월지) — 계절 지배력. 원국에 없는 기운이 여기서 들어온다</li>
 *   <li>오행 분포와 음양 비율을 <b>8글자 기준</b>으로 다시 계산</li>
 *   <li>신강·신약 판정과 그 근거(득령·득지·득세)</li>
 * </ul>
 *
 * <p><b>신강·신약은 판정법이 학파마다 다르다.</b> 여기서는 가장 널리 쓰이는 골격만 쓴다 —
 * 일간을 돕는 글자(비겁·인성)와 빼는 글자(식상·재성·관성)를 세고, 월지에 두 배를 준다.
 * AI가 이 값을 단정적으로 쓰지 않도록 근거를 함께 내보내고, 애매한 구간은 '중화'로 표시한다.
 */
@Component
public class SajuAnalyzer {

    /** 십신 이름. 인덱스 의미는 sipsin() 참고. */
    private static final String BIGYEON = "비견";
    private static final String GEOPJAE = "겁재";
    private static final String SIKSIN = "식신";
    private static final String SANGGWAN = "상관";
    private static final String PYEONJAE = "편재";
    private static final String JEONGJAE = "정재";
    private static final String PYEONGWAN = "편관";
    private static final String JEONGGWAN = "정관";
    private static final String PYEONIN = "편인";
    private static final String JEONGIN = "정인";

    /** 일간을 돕는 십신(비겁·인성). 신강 판정에서 더하는 쪽. */
    private static boolean supports(String sipsin) {
        return BIGYEON.equals(sipsin) || GEOPJAE.equals(sipsin)
                || PYEONIN.equals(sipsin) || JEONGIN.equals(sipsin);
    }

    /** 8글자 한 칸. 화면·프롬프트에 그대로 나갈 모양. */
    public record Gan(String position, String korean, String hanja,
                      String element, String yinYang, String sipsin) {
    }

    /** 신강·신약 판정 결과와 근거. */
    public record Strength(String verdict, int score,
                           boolean deukryeong, boolean deukji, boolean deukse, String basis) {
    }

    /** 분석 전체. */
    public record Profile(List<Gan> chars, String ilgan, String ilganElement, String ilganYinYang,
                          String wolryeong, String wolryeongElement,
                          List<Orb> orbs, Map<String, Integer> yinYangCount,
                          Strength strength) {
    }

    public Profile analyze(SajuCalculator.Palja palja) {
        char ilgan = SajuTables.koreanOf(palja.day().cheongan());
        Ohaeng ilganEl = SajuTables.CHEONGAN.get(ilgan);
        boolean ilganYang = SajuTables.isYangCheongan(ilgan);

        List<Gan> chars = new ArrayList<>();
        addPillar(chars, "년", palja.year(), ilgan);
        addPillar(chars, "월", palja.month(), ilgan);
        addPillar(chars, "일", palja.day(), ilgan);
        addPillar(chars, "시", palja.hour(), ilgan);

        char wolji = SajuTables.koreanOf(palja.month().jiji());

        return new Profile(
                chars,
                String.valueOf(ilgan),
                elementName(ilganEl),
                ilganYang ? "양" : "음",
                wolji == 0 ? null : String.valueOf(wolji),
                wolji == 0 ? null : elementName(SajuTables.JIJI.get(wolji)),
                orbs(chars, wolji),
                yinYangCount(chars),
                strength(chars, wolji)
        );
    }

    private void addPillar(List<Gan> out, String position, SajuCalculator.PaljaPillar pillar, char ilgan) {
        char gan = SajuTables.koreanOf(pillar.cheongan());
        char ji = SajuTables.koreanOf(pillar.jiji());
        if (gan != 0) {
            out.add(new Gan(position + "간", String.valueOf(gan), pillar.cheongan(),
                    elementName(SajuTables.CHEONGAN.get(gan)),
                    SajuTables.isYangCheongan(gan) ? "양" : "음",
                    sipsin(ilgan, SajuTables.CHEONGAN.get(gan), SajuTables.isYangCheongan(gan))));
        }
        if (ji != 0) {
            out.add(new Gan(position + "지", String.valueOf(ji), pillar.jiji(),
                    elementName(SajuTables.JIJI.get(ji)),
                    SajuTables.isYangJiji(ji) ? "양" : "음",
                    sipsin(ilgan, SajuTables.JIJI.get(ji), SajuTables.isYangJiji(ji))));
        }
    }

    /**
     * 십신. 일간의 오행·음양과 상대 글자의 오행·음양을 견준다.
     * 음양이 같으면 편(偏)·비견·식신 계열, 다르면 정(正)·겁재·상관 계열로 갈린다.
     * 여기가 "같은 목이어도 갑목과 을목이 다르다"가 실제로 드러나는 자리다.
     */
    private String sipsin(char ilgan, Ohaeng target, boolean targetYang) {
        Ohaeng me = SajuTables.CHEONGAN.get(ilgan);
        if (me == null || target == null) {
            return null;
        }
        boolean same = SajuTables.isYangCheongan(ilgan) == targetYang;
        if (me == target) {
            return same ? BIGYEON : GEOPJAE;
        }
        if (SajuTables.generates(me, target)) {          // 내가 생한다 → 식상
            return same ? SIKSIN : SANGGWAN;
        }
        if (SajuTables.controls(me, target)) {           // 내가 극한다 → 재성
            return same ? PYEONJAE : JEONGJAE;
        }
        if (SajuTables.controls(target, me)) {           // 나를 극한다 → 관성
            return same ? PYEONGWAN : JEONGGWAN;
        }
        return same ? PYEONIN : JEONGIN;                 // 나를 생한다 → 인성
    }

    /**
     * 오행 분포. 8글자를 모두 세고 월지에만 가중을 더한다(월령이 계절을 지배하므로).
     * 시간을 모르면 시주 두 글자가 빠지고, 합으로 정규화하므로 비율은 그대로 유효하다.
     */
    private List<Orb> orbs(List<Gan> chars, char wolji) {
        Map<Ohaeng, Double> w = new EnumMap<>(Ohaeng.class);
        for (Ohaeng o : Ohaeng.values()) {
            w.put(o, 0.0);
        }
        for (Gan g : chars) {
            Ohaeng el = elementOf(g.element());
            if (el == null) {
                continue;
            }
            double weight = g.position().equals("월지") ? 1.5 : 1.0;
            w.merge(el, weight, Double::sum);
        }
        double total = w.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Orb> out = new ArrayList<>();
        for (Ohaeng o : Ohaeng.values()) {
            out.add(new Orb(o.hanja(), total == 0 ? 0 : Math.round(w.get(o) / total * 100.0) / 100.0));
        }
        return out;
    }

    private Map<String, Integer> yinYangCount(List<Gan> chars) {
        int yang = 0;
        for (Gan g : chars) {
            if ("양".equals(g.yinYang())) {
                yang++;
            }
        }
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("양", yang);
        m.put("음", chars.size() - yang);
        return m;
    }

    /**
     * 신강·신약. 일간을 돕는 글자는 +1, 힘을 빼는 글자는 −1, 월지는 두 배.
     * 일간 자신은 세지 않는다(기준이므로).
     */
    private Strength strength(List<Gan> chars, char wolji) {
        int score = 0;
        boolean deukryeong = false;
        boolean deukji = false;
        int otherSupport = 0;
        int otherTotal = 0;

        for (Gan g : chars) {
            if ("일간".equals(g.position()) || g.sipsin() == null) {
                continue;
            }
            boolean helps = supports(g.sipsin());
            int weight = "월지".equals(g.position()) ? 2 : 1;
            score += helps ? weight : -weight;

            if ("월지".equals(g.position())) {
                deukryeong = helps;
            } else if ("일지".equals(g.position())) {
                deukji = helps;
            } else {
                otherTotal++;
                if (helps) {
                    otherSupport++;
                }
            }
        }
        boolean deukse = otherTotal > 0 && otherSupport * 2 >= otherTotal;

        String verdict = score >= 2 ? "신강" : score <= -2 ? "신약" : "중화";
        String basis = "득령(월지가 일간을 돕는가) %s · 득지(일지) %s · 득세(나머지 글자) %s"
                .formatted(deukryeong ? "○" : "×", deukji ? "○" : "×", deukse ? "○" : "×");
        return new Strength(verdict, score, deukryeong, deukji, deukse, basis);
    }

    private static String elementName(Ohaeng o) {
        return o == null ? null : o.hanja();
    }

    private static Ohaeng elementOf(String hanja) {
        for (Ohaeng o : Ohaeng.values()) {
            if (o.hanja().equals(hanja)) {
                return o;
            }
        }
        return null;
    }
}
