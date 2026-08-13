package com.lucky.fortune.saju;

import com.github.usingsky.calendar.KoreanLunarCalendar;
import com.lucky.fortune.dto.Subject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 결정적 사주 계산. AI에 "사실"로 넘길 값들을 만든다.
 *
 * <p>v1 범위: <b>일주(일간+일지)</b>는 60갑자로 정확히 계산(양/음력 모두, KoreanLunarCalendar).
 * <b>오행 분포</b>는 일간+일지+계절(양력 월) 기반 근사. 년/월/시주는 절기·설 경계 모호로 v1에서 제외.
 * 궁합 수치(syncRate/temperature/distancePos)는 두 일주 관계 기반 결정적 휴리스틱(상수 조정 가능).
 */
@Component
public class SajuCalculator {

    /** 한 사람의 계산 결과(내부 표현). */
    public record Pillar(char ilgan, char ilji, int solarYear, int solarMonth, int solarDay) {
    }

    /** 팔자 한 기둥(천간/지지, 한자). 시주를 모르면 두 값 빈 문자열. */
    public record PaljaPillar(String cheongan, String jiji) {
        static PaljaPillar empty() {
            return new PaljaPillar("", "");
        }
    }

    /** 팔자 4기둥 + 일주 라벨. cheongan/jiji는 한자 한 글자. */
    public record Palja(PaljaPillar year, PaljaPillar month, PaljaPillar day,
                        PaljaPillar hour, String iljuName) {
    }

    /**
     * 팔자 4기둥 계산(AI·저장 없음). 연·월·일주는 KoreanLunarCalendar(절기 반영),
     * 시주는 오자시법(일간 기준)+태어난 시지로 계산. 시간 모르면 시주는 빈 값.
     */
    public Palja palja(Subject s) {
        int[] ymd = parseYmd(s.birthDate());
        KoreanLunarCalendar cal = KoreanLunarCalendar.getInstance();
        boolean ok = "음력".equals(s.calendar())
                ? cal.setLunarDate(ymd[0], ymd[1], ymd[2], s.leapMonth())
                : cal.setSolarDate(ymd[0], ymd[1], ymd[2]);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "변환할 수 없는 날짜입니다(지원 1000~2050): " + s.birthDate());
        }
        // "정유년 병오월 임오일 (윤월)" → [정유, 병오, 임오]
        String gapja = cal.getGapjaString().trim();
        String yearKo = pillarToken(gapja, "년");
        String monthKo = pillarToken(gapja, "월");
        String dayKo = pillarToken(gapja, "일");
        char ilgan = dayKo.charAt(0);

        PaljaPillar hour = hourPillar(ilgan, s);
        String iljuName = dayKo + "일주";   // 예: "임오일주"
        return new Palja(
                toHanjaPillar(yearKo),
                toHanjaPillar(monthKo),
                toHanjaPillar(dayKo),
                hour,
                iljuName
        );
    }

    /** 시주 = 오자시법(일간→자시 천간) + 태어난 시지. 시간 모르면 빈 값. */
    private PaljaPillar hourPillar(char ilgan, Subject s) {
        if (s.timeUnknown() || s.birthTime() == null || s.birthTime().isBlank()) {
            return PaljaPillar.empty();
        }
        char siji = s.birthTime().trim().charAt(0);   // "오시 (...)" → '오'
        int sijiIdx = indexOf(SajuTables.JIJI_ORDER, siji);
        if (sijiIdx < 0) {
            return PaljaPillar.empty();
        }
        int ganIdx = (SajuTables.jasiCheonganIndex(ilgan) + sijiIdx) % 10;
        char gan = SajuTables.CHEONGAN_ORDER[ganIdx];
        Character ganH = SajuTables.CHEONGAN_HANJA.get(gan);
        Character jiH = SajuTables.JIJI_HANJA.get(siji);
        if (ganH == null || jiH == null) {
            return PaljaPillar.empty();
        }
        return new PaljaPillar(ganH.toString(), jiH.toString());
    }

    /** "정유년 병오월 임오일 (윤월)"에서 접미사(년/월/일)로 끝나는 토큰의 앞 2글자. */
    private static String pillarToken(String gapja, String suffix) {
        for (String tok : gapja.split("\\s+")) {
            if (tok.endsWith(suffix) && tok.length() >= 3) {
                return tok.substring(0, 2);
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "팔자 파싱 실패(" + suffix + "): " + gapja);
    }

    /** "임오"(한글 2글자) → PaljaPillar(한자). */
    private static PaljaPillar toHanjaPillar(String ko) {
        Character gan = SajuTables.CHEONGAN_HANJA.get(ko.charAt(0));
        Character ji = SajuTables.JIJI_HANJA.get(ko.charAt(1));
        return new PaljaPillar(gan == null ? "" : gan.toString(), ji == null ? "" : ji.toString());
    }

    private static int indexOf(char[] arr, char c) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == c) return i;
        }
        return -1;
    }

    /** Subject → 일주 + 양력월(계절) 계산. */
    public Pillar pillar(Subject s) {
        int[] ymd = parseYmd(s.birthDate());
        KoreanLunarCalendar cal = KoreanLunarCalendar.getInstance();
        boolean ok = "음력".equals(s.calendar())
                ? cal.setLunarDate(ymd[0], ymd[1], ymd[2], s.leapMonth())
                : cal.setSolarDate(ymd[0], ymd[1], ymd[2]);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "변환할 수 없는 날짜입니다(지원 1000~2050): " + s.birthDate());
        }
        String ilju = extractIlju(cal.getGapjaString());   // "갑자"
        int[] solar = parseYmd(cal.getSolarIsoFormat());    // 음력이면 변환된 양력
        return new Pillar(ilju.charAt(0), ilju.charAt(1), solar[0], solar[1], solar[2]);
    }

    /** "1999.11.11 · 양력 · 정묘 일주" 표시 문자열. */
    public String iljuLabel(Subject s, Pillar p) {
        int[] ymd = parseYmd(s.birthDate());
        return "%04d.%02d.%02d · %s · %c%c 일주"
                .formatted(ymd[0], ymd[1], ymd[2], s.calendar(), p.ilgan(), p.ilji());
    }

    /** 오행 분포(v1): 일간 + 일지 + 계절 → 5칸 0~1(합 1). */
    public List<Orb> orbs(Pillar p) {
        Map<Ohaeng, Double> w = new EnumMap<>(Ohaeng.class);
        for (Ohaeng o : Ohaeng.values()) {
            w.put(o, 0.0);
        }
        add(w, SajuTables.CHEONGAN.get(p.ilgan()), 1.0);  // 일간(본인) 가중
        add(w, SajuTables.JIJI.get(p.ilji()), 1.0);
        add(w, seasonElement(p.solarMonth()), 0.8);       // 계절 보정(근사)
        double total = w.values().stream().mapToDouble(Double::doubleValue).sum();
        List<Orb> orbs = new ArrayList<>();
        for (Ohaeng o : Ohaeng.values()) {
            orbs.add(new Orb(o.hanja(), total == 0 ? 0 : round2(w.get(o) / total)));
        }
        return orbs;
    }

    // ── 2인 궁합 수치 (휴리스틱, v1 — 상수 조정 가능) ──

    public int syncRate(Pillar a, Pillar b) {
        int s = 50;
        if (SajuTables.contains(SajuTables.CHEONGAN_HAP, a.ilgan(), b.ilgan())) s += 20;
        if (SajuTables.contains(SajuTables.CHEONGAN_CHUNG, a.ilgan(), b.ilgan())) s -= 15;
        if (SajuTables.contains(SajuTables.JIJI_HAP, a.ilji(), b.ilji())) s += 15;
        if (sameSamhap(a.ilji(), b.ilji())) s += 12;
        if (SajuTables.contains(SajuTables.JIJI_CHUNG, a.ilji(), b.ilji())) s -= 15;
        s += saengGeuk(elementOf(a.ilgan()), elementOf(b.ilgan()));
        return clamp(s);
    }

    public int temperature(Pillar a, Pillar b) {
        int t = 45;
        t += fireCount(a, b) * 8;
        if (SajuTables.contains(SajuTables.CHEONGAN_HAP, a.ilgan(), b.ilgan())) t += 10;
        if (SajuTables.contains(SajuTables.JIJI_HAP, a.ilji(), b.ilji())) t += 8;
        if (SajuTables.contains(SajuTables.JIJI_CHUNG, a.ilji(), b.ilji())) t -= 10;
        return clamp(t);
    }

    public int distancePos(Pillar a, Pillar b) {
        int d = 50;
        if (SajuTables.contains(SajuTables.JIJI_CHUNG, a.ilji(), b.ilji())) d += 20;
        if (SajuTables.contains(SajuTables.CHEONGAN_CHUNG, a.ilgan(), b.ilgan())) d += 12;
        if (SajuTables.contains(SajuTables.JIJI_HAP, a.ilji(), b.ilji())) d -= 18;
        if (SajuTables.contains(SajuTables.CHEONGAN_HAP, a.ilgan(), b.ilgan())) d -= 18;
        d -= saengGeuk(elementOf(a.ilgan()), elementOf(b.ilgan()));  // 생이면 가까이
        return clamp(d);
    }

    // ── helpers ──

    private static Ohaeng elementOf(char cheongan) {
        return SajuTables.CHEONGAN.get(cheongan);
    }

    /** 같으면 비화(+5), 상생 +8, 상극 -8, 그 외 0. */
    private static int saengGeuk(Ohaeng a, Ohaeng b) {
        if (a == null || b == null) return 0;
        if (a == b) return 5;
        if (generates(a, b) || generates(b, a)) return 8;
        if (controls(a, b) || controls(b, a)) return -8;
        return 0;
    }

    /** 목→화→토→금→수→목 */
    private static boolean generates(Ohaeng a, Ohaeng b) {
        return (a == Ohaeng.MOK && b == Ohaeng.HWA) || (a == Ohaeng.HWA && b == Ohaeng.TO)
                || (a == Ohaeng.TO && b == Ohaeng.GEUM) || (a == Ohaeng.GEUM && b == Ohaeng.SU)
                || (a == Ohaeng.SU && b == Ohaeng.MOK);
    }

    /** 목극토, 토극수, 수극화, 화극금, 금극목 */
    private static boolean controls(Ohaeng a, Ohaeng b) {
        return (a == Ohaeng.MOK && b == Ohaeng.TO) || (a == Ohaeng.TO && b == Ohaeng.SU)
                || (a == Ohaeng.SU && b == Ohaeng.HWA) || (a == Ohaeng.HWA && b == Ohaeng.GEUM)
                || (a == Ohaeng.GEUM && b == Ohaeng.MOK);
    }

    private static boolean sameSamhap(char a, char b) {
        for (var g : SajuTables.JIJI_SAMHAP) {
            if (g.contains(a) && g.contains(b)) return true;
        }
        return false;
    }

    private static int fireCount(Pillar a, Pillar b) {
        int c = 0;
        if (SajuTables.CHEONGAN.get(a.ilgan()) == Ohaeng.HWA) c++;
        if (SajuTables.CHEONGAN.get(b.ilgan()) == Ohaeng.HWA) c++;
        if (SajuTables.JIJI.get(a.ilji()) == Ohaeng.HWA) c++;
        if (SajuTables.JIJI.get(b.ilji()) == Ohaeng.HWA) c++;
        return c;
    }

    /** 계절 근사: 봄(2~4)木 여름(5~7)火 가을(8~10)金 겨울(11~1)水. */
    private static Ohaeng seasonElement(int month) {
        return switch (month) {
            case 2, 3, 4 -> Ohaeng.MOK;
            case 5, 6, 7 -> Ohaeng.HWA;
            case 8, 9, 10 -> Ohaeng.GEUM;
            default -> Ohaeng.SU;  // 11, 12, 1
        };
    }

    private static void add(Map<Ohaeng, Double> w, Ohaeng o, double v) {
        if (o != null) w.merge(o, v, Double::sum);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }

    private static int[] parseYmd(String yyyyMmDd) {
        String[] p = yyyyMmDd.split("-");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    /** "임인년 정미월 갑자일 (윤월)" → "갑자" (일로 끝나는 토큰의 앞 2글자). */
    private static String extractIlju(String gapja) {
        for (String tok : gapja.trim().split("\\s+")) {
            if (tok.endsWith("일") && tok.length() >= 3) {
                return tok.substring(0, 2);
            }
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "일주 파싱 실패: " + gapja);
    }
}
