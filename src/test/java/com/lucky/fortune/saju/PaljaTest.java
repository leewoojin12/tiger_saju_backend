package com.lucky.fortune.saju;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lucky.fortune.dto.Subject;
import org.junit.jupiter.api.Test;

class PaljaTest {

    private final SajuCalculator calc = new SajuCalculator();

    @Test
    void palja_solar_with_time() {
        // 양력 2017-06-24, 오시 → 정유년 병오월 임오일 + 시주(임일+오시=병오시)
        Subject s = new Subject("me", "테스트", "2017-06-24", "양력", false, "오시 (11–13시)", false);
        var p = calc.palja(s);
        assertEquals("丁", p.year().cheongan());
        assertEquals("酉", p.year().jiji());
        assertEquals("丙", p.month().cheongan());
        assertEquals("午", p.month().jiji());
        assertEquals("壬", p.day().cheongan());
        assertEquals("午", p.day().jiji());
        // 임일 자시천간=경(idx6), 오시 지지 idx6 → 경+6=병, 시지 오
        assertEquals("丙", p.hour().cheongan());
        assertEquals("午", p.hour().jiji());
        assertEquals("임오일주", p.iljuName());
    }

    @Test
    void palja_time_unknown_empty_hour() {
        Subject s = new Subject("me", "테스트", "2017-06-24", "양력", false, null, true);
        var p = calc.palja(s);
        assertEquals("", p.hour().cheongan());
        assertEquals("", p.hour().jiji());
        // 일주는 여전히 계산됨
        assertEquals("壬", p.day().cheongan());
    }
}
