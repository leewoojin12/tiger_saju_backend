package com.lucky.fortune.saju;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 24절기 계산. 태양의 겉보기 황경(黃經)으로 구한다.
 *
 * <p><b>왜 필요한가.</b> 사주의 해와 달은 달력이 아니라 절기로 바뀐다.
 * 해는 입춘(황경 315°)에, 달은 12개 절(節)에 바뀐다. 음력 초하루나 설날과는 무관하다.
 * 쓰던 음력 변환 라이브러리는 음력 기준으로 간지를 넘겨서, 년주와 월주가 어긋나 있었다.
 *
 * <p><b>정확도(실측).</b> Meeus 『Astronomical Algorithms』 25장의 간이식을 쓴다.
 * VSOP87 기반 계산(pyephem)과 1900~2100년 24절기 4824건을 전부 대조한 결과,
 * 절입 <b>시각</b>은 최대 23분, 절입 <b>날짜</b>가 갈린 건 20건(0.4%)이었다.
 * 날짜가 갈린 건 전부 절입이 자정 근처였던 경우다.
 *
 * <p>이 오차를 안고 가는 이유는, 더 큰 오차원이 입력 쪽에 있기 때문이다.
 * 이 서비스는 출생 시각을 <b>시지(2시간 단위)</b>로만 받는다. 즉 입력 자체가 ±60분이라
 * 여기서 23분을 더 줄여도 결과가 달라지지 않는다. 분 단위 출생 시각과 출생지 경도(진태양시)를
 * 받게 되면 그때 태양 위치 계산을 VSOP87 급으로 올리는 것이 순서다.
 *
 * <p>계산은 역학시(TT) 기준이라 세계시(UT)로 바꿀 때 ΔT 를 빼 준다. 1900~2100 구간에서
 * ΔT 는 1분 안팎이라 결과에 거의 영향이 없지만, 빼는 편이 옳다.
 */
public final class SolarTerms {

    private SolarTerms() {
    }

    /** 한국 표준시. 1908~1961 사이 표준자오선이 몇 차례 바뀌었으나, 여기서는 현행 +9 로 통일한다. */
    public static final ZoneOffset KST = ZoneOffset.ofHours(9);

    /** 인월(寅月)이 시작되는 황경 = 입춘. 여기서부터 30°마다 달이 바뀐다. */
    private static final double IPCHUN_LONGITUDE = 315.0;

    /**
     * 이 순간이 사주의 몇 번째 달인지. 0 = 인월(입춘~경칩), 1 = 묘월, … 11 = 축월.
     * 지지 순서(자축인묘…)가 아니라 <b>인월부터</b>라는 점에 주의.
     */
    public static int monthBranchIndex(LocalDateTime kst) {
        double lambda = apparentSolarLongitude(julianDayTT(kst));
        double fromIpchun = (lambda - IPCHUN_LONGITUDE + 360.0) % 360.0;
        return (int) Math.floor(fromIpchun / 30.0);
    }

    /**
     * 이 순간이 속한 사주 연도(입춘 기준).
     *
     * <p>입춘 전에 태어났으면 달력상 해가 바뀌었어도 전년으로 본다.
     * 자월(대설~소한)은 12월과 1월에 걸쳐 있어서 달만 봐서는 갈리지 않으므로,
     * 달력 월을 함께 본다 — 1·2월에 자월이나 축월이면 아직 입춘 전이다.
     */
    public static int solarYear(LocalDateTime kst) {
        int monthIndex = monthBranchIndex(kst);
        boolean beforeIpchun = kst.getMonthValue() <= 2 && monthIndex >= 10;   // 10=자월, 11=축월
        return beforeIpchun ? kst.getYear() - 1 : kst.getYear();
    }

    /**
     * 지정한 황경에 태양이 도달하는 순간(KST). 검증과 안내 문구용.
     * 이분법으로 좁힌다 — 황경은 단조 증가라 안전하고, 반복 60회면 밀리초 수준으로 수렴한다.
     *
     * @param year          찾을 달력 연도
     * @param targetDegrees 0~359. 입춘 315, 경칩 345, 청명 15 …
     */
    public static LocalDateTime termInstant(int year, double targetDegrees) {
        double target = norm(targetDegrees);
        // 태양은 1월 1일 무렵 황경 280° 근처에 있다. 목표까지의 각도를 날짜로 환산해
        // 대략의 시점을 잡고 앞뒤 30일만 훑는다. 한 해 전체를 훑으면 연말·연초 절기에서
        // 이웃 해의 것을 집어 온다(동지를 찾았더니 전년 12월이 나오던 문제).
        double approxDayOfYear = ((target - 280.0 + 360.0) % 360.0) * 365.2422 / 360.0;
        LocalDateTime guess = LocalDateTime.of(year, 1, 1, 0, 0)
                .plusSeconds(Math.round(approxDayOfYear * 86400.0));
        double lo = julianDayTT(guess.minusDays(30));
        double hi = julianDayTT(guess.plusDays(30));

        // 목표 황경을 기준으로 -180~+180 으로 펼치면 구간 안에서 단조 증가한다.
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            if (signedDiff(apparentSolarLongitude(mid), target) < 0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return fromJulianDayTT((lo + hi) / 2.0);
    }

    /** a - target 을 -180~+180 으로 접은 값. */
    private static double signedDiff(double a, double target) {
        double d = (a - target + 540.0) % 360.0 - 180.0;
        return d;
    }

    // ── 천문 계산 ──────────────────────────────────────────────

    /** 태양의 겉보기 황경(도, 0~360). Meeus 25장 간이식. */
    static double apparentSolarLongitude(double jde) {
        double t = (jde - 2451545.0) / 36525.0;
        // 기하 평균 황경
        double l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t;
        // 평균 근점이각
        double m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t;
        double mRad = Math.toRadians(norm(m));
        // 중심차
        double c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(mRad)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * mRad)
                + 0.000289 * Math.sin(3 * mRad);
        double trueLongitude = l0 + c;
        // 장동·광행차 보정 → 겉보기 황경
        double omega = 125.04 - 1934.136 * t;
        double apparent = trueLongitude - 0.00569 - 0.00478 * Math.sin(Math.toRadians(norm(omega)));
        return norm(apparent);
    }

    private static double norm(double deg) {
        double d = deg % 360.0;
        return d < 0 ? d + 360.0 : d;
    }

    /** KST 시각 → 율리우스일(역학시 TT). */
    static double julianDayTT(LocalDateTime kst) {
        double jdUt = julianDay(kst.minusHours(9));
        return jdUt + deltaTSeconds(kst.getYear()) / 86400.0;
    }

    /** 율리우스일(TT) → KST 시각. */
    static LocalDateTime fromJulianDayTT(double jdTt) {
        // ΔT 는 연도에 따라 달라지지만, 먼저 대략의 연도를 얻어 한 번만 보정하면 충분하다.
        LocalDateTime rough = fromJulianDay(jdTt).plusHours(9);
        double jdUt = jdTt - deltaTSeconds(rough.getYear()) / 86400.0;
        return fromJulianDay(jdUt).plusHours(9);
    }

    /** 그레고리력 UTC → 율리우스일. */
    private static double julianDay(LocalDateTime utc) {
        int y = utc.getYear();
        int mo = utc.getMonthValue();
        if (mo <= 2) {
            y -= 1;
            mo += 12;
        }
        int a = y / 100;
        int b = 2 - a + a / 4;
        double dayFraction = utc.getDayOfMonth()
                + (utc.getHour() + (utc.getMinute() + utc.getSecond() / 60.0) / 60.0) / 24.0;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (mo + 1))
                + dayFraction + b - 1524.5;
    }

    /** 율리우스일 → 그레고리력 UTC. */
    private static LocalDateTime fromJulianDay(double jd) {
        double z = Math.floor(jd + 0.5);
        double f = jd + 0.5 - z;
        double alpha = Math.floor((z - 1867216.25) / 36524.25);
        double a = z + 1 + alpha - Math.floor(alpha / 4);
        double b = a + 1524;
        double c = Math.floor((b - 122.1) / 365.25);
        double d = Math.floor(365.25 * c);
        double e = Math.floor((b - d) / 30.6001);

        double dayWithFraction = b - d - Math.floor(30.6001 * e) + f;
        int day = (int) Math.floor(dayWithFraction);
        int month = (int) (e < 14 ? e - 1 : e - 13);
        int year = (int) (month > 2 ? c - 4716 : c - 4715);

        double secondsOfDay = (dayWithFraction - day) * 86400.0;
        long seconds = Math.round(secondsOfDay);
        return LocalDateTime.of(year, month, day, 0, 0).plusSeconds(seconds);
    }

    /**
     * ΔT(TT − UT), 초. Espenak·Meeus 다항 근사에서 이 서비스가 다루는 구간만 추렸다.
     * 1900년 이전은 그대로 쓰기 어렵지만, 사주 계산 대상 연도가 아니므로 경계값으로 둔다.
     */
    private static double deltaTSeconds(int year) {
        if (year < 1900) {
            return -2.0;
        }
        if (year < 1920) {
            double t = year - 1900;
            return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t
                    - 0.000197 * t * t * t * t;
        }
        if (year < 1941) {
            double t = year - 1920;
            return 21.20 + 0.84493 * t - 0.076100 * t * t + 0.0020936 * t * t * t;
        }
        if (year < 1961) {
            double t = year - 1950;
            return 29.07 + 0.407 * t - t * t / 233.0 + t * t * t / 2547.0;
        }
        if (year < 1986) {
            double t = year - 1975;
            return 45.45 + 1.067 * t - t * t / 260.0 - t * t * t / 718.0;
        }
        if (year < 2005) {
            double t = year - 2000;
            return 63.86 + 0.3345 * t - 0.060374 * t * t + 0.0017275 * t * t * t
                    + 0.000651814 * t * t * t * t + 0.00002373599 * t * t * t * t * t;
        }
        if (year < 2050) {
            double t = year - 2000;
            return 62.92 + 0.32217 * t + 0.005589 * t * t;
        }
        double t = year - 1820;
        return -20 + 32 * (t / 100.0) * (t / 100.0) - 0.5628 * (2150 - year);
    }
}
