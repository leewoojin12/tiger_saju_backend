package com.lucky.fortune.saju;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** 사주 계산용 정적 매핑(천간/지지 → 오행, 합/충/삼합 관계). v1 근사용. */
final class SajuTables {

    private SajuTables() {
    }

    /** 천간(10) → 오행 */
    static final Map<Character, Ohaeng> CHEONGAN = Map.ofEntries(
            Map.entry('갑', Ohaeng.MOK), Map.entry('을', Ohaeng.MOK),
            Map.entry('병', Ohaeng.HWA), Map.entry('정', Ohaeng.HWA),
            Map.entry('무', Ohaeng.TO), Map.entry('기', Ohaeng.TO),
            Map.entry('경', Ohaeng.GEUM), Map.entry('신', Ohaeng.GEUM),
            Map.entry('임', Ohaeng.SU), Map.entry('계', Ohaeng.SU)
    );

    /** 지지(12) → 오행 */
    static final Map<Character, Ohaeng> JIJI = Map.ofEntries(
            Map.entry('자', Ohaeng.SU), Map.entry('축', Ohaeng.TO),
            Map.entry('인', Ohaeng.MOK), Map.entry('묘', Ohaeng.MOK),
            Map.entry('진', Ohaeng.TO), Map.entry('사', Ohaeng.HWA),
            Map.entry('오', Ohaeng.HWA), Map.entry('미', Ohaeng.TO),
            Map.entry('신', Ohaeng.GEUM), Map.entry('유', Ohaeng.GEUM),
            Map.entry('술', Ohaeng.TO), Map.entry('해', Ohaeng.SU)
    );

    /** 천간 합 (甲己 乙庚 丙辛 丁壬 戊癸) */
    static final Set<String> CHEONGAN_HAP = Set.of("갑기", "을경", "병신", "정임", "무계");
    /** 천간 충 (甲庚 乙辛 丙壬 丁癸) */
    static final Set<String> CHEONGAN_CHUNG = Set.of("갑경", "을신", "병임", "정계");
    /** 지지 육합 */
    static final Set<String> JIJI_HAP = Set.of("자축", "인해", "묘술", "진유", "사신", "오미");
    /** 지지 충 */
    static final Set<String> JIJI_CHUNG = Set.of("자오", "축미", "인신", "묘유", "진술", "사해");
    /** 지지 삼합 그룹 (申子辰 寅午戌 亥卯未 巳酉丑) */
    static final List<Set<Character>> JIJI_SAMHAP = List.of(
            Set.of('신', '자', '진'), Set.of('인', '오', '술'),
            Set.of('해', '묘', '미'), Set.of('사', '유', '축')
    );

    /** 두 글자 쌍이 셋에 있는지(순서 무관). */
    static boolean contains(Set<String> set, char a, char b) {
        return set.contains("" + a + b) || set.contains("" + b + a);
    }

    // ── 팔자(4기둥) 계산용 ──

    /** 천간 10개 순서(갑→계). */
    static final char[] CHEONGAN_ORDER = {'갑', '을', '병', '정', '무', '기', '경', '신', '임', '계'};
    /** 지지 12개 순서(자→해). */
    static final char[] JIJI_ORDER = {'자', '축', '인', '묘', '진', '사', '오', '미', '신', '유', '술', '해'};

    /** 한글 천간 → 한자. */
    static final Map<Character, Character> CHEONGAN_HANJA = Map.ofEntries(
            Map.entry('갑', '甲'), Map.entry('을', '乙'), Map.entry('병', '丙'),
            Map.entry('정', '丁'), Map.entry('무', '戊'), Map.entry('기', '己'),
            Map.entry('경', '庚'), Map.entry('신', '辛'), Map.entry('임', '壬'),
            Map.entry('계', '癸')
    );
    /** 한글 지지 → 한자. */
    static final Map<Character, Character> JIJI_HANJA = Map.ofEntries(
            Map.entry('자', '子'), Map.entry('축', '丑'), Map.entry('인', '寅'),
            Map.entry('묘', '卯'), Map.entry('진', '辰'), Map.entry('사', '巳'),
            Map.entry('오', '午'), Map.entry('미', '未'), Map.entry('신', '申'),
            Map.entry('유', '酉'), Map.entry('술', '戌'), Map.entry('해', '亥')
    );

    /**
     * 오자시법: 일간 → 자시(子)의 천간 인덱스(CHEONGAN_ORDER 기준).
     * 갑기→갑(0), 을경→병(2), 병신→무(4), 정임→경(6), 무계→임(8).
     */
    static int jasiCheonganIndex(char ilgan) {
        return switch (ilgan) {
            case '갑', '기' -> 0;
            case '을', '경' -> 2;
            case '병', '신' -> 4;
            case '정', '임' -> 6;
            case '무', '계' -> 8;
            default -> 0;
        };
    }
}
