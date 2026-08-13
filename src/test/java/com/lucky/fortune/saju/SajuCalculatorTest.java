package com.lucky.fortune.saju;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lucky.fortune.dto.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;

class SajuCalculatorTest {

    private final SajuCalculator calc = new SajuCalculator();

    private static Subject solar(String date) {
        return new Subject("me", "테스트", date, "양력", false, null, true);
    }

    private static Subject lunar(String date) {
        return new Subject("me", "테스트", date, "음력", false, null, true);
    }

    @Test
    void ilju_solar_known() {
        // 양력 2017-06-24 → "정유년 병오월 임오일" → 일주 임오
        var p = calc.pillar(solar("2017-06-24"));
        assertEquals('임', p.ilgan());
        assertEquals('오', p.ilji());
    }

    @Test
    void ilju_lunar_known() {
        // 음력 1956-01-21 → 양력 1956-03-03 "병신년 경인월 기사일" → 일주 기사
        var p = calc.pillar(lunar("1956-01-21"));
        assertEquals('기', p.ilgan());
        assertEquals('사', p.ilji());
    }

    @Test
    void orbs_sum_to_one() {
        var orbs = calc.orbs(calc.pillar(solar("1999-11-11")));
        assertEquals(5, orbs.size());
        double sum = orbs.stream().mapToDouble(Orb::v).sum();
        assertTrue(Math.abs(sum - 1.0) < 0.05, "orbs 합이 1 근처여야: " + sum);
    }

    @Test
    void metrics_in_range() {
        var a = calc.pillar(solar("1999-11-11"));
        var b = calc.pillar(solar("2000-03-05"));
        for (int v : List.of(calc.syncRate(a, b), calc.temperature(a, b), calc.distancePos(a, b))) {
            assertTrue(v >= 0 && v <= 100, "0~100 범위여야: " + v);
        }
    }
}
