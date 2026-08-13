package com.lucky.saju.service;

import com.lucky.fortune.dto.Subject;
import com.lucky.fortune.saju.SajuCalculator;
import com.lucky.saju.dto.PaljaResponse;
import com.lucky.saju.dto.SajuRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 팔자판 전용 경량 계산. AI 호출도, 보관함 저장도 하지 않는다.
 * 화면(맛보기/풀 리포트)에서 사주팔자 8글자만 빠르게 그릴 때 사용.
 */
@Service
@RequiredArgsConstructor
public class PaljaService {

    private final SajuCalculator calc;

    public PaljaResponse compute(SajuRequest req) {
        Subject subject = new Subject(
                "me",
                req.name(),
                req.birthDate().toString(),   // LocalDate → "yyyy-MM-dd"
                req.calendar(),
                false,                          // 윤달 입력 없음(기존 /api/saju와 동일)
                req.birthTime(),
                req.timeUnknown()
        );
        SajuCalculator.Palja p = calc.palja(subject);
        return new PaljaResponse(
                new PaljaResponse.Ilju(p.iljuName()),
                new PaljaResponse.Palja(
                        toDto(p.hour()),
                        toDto(p.day()),
                        toDto(p.month()),
                        toDto(p.year())
                )
        );
    }

    private static PaljaResponse.Pillar toDto(SajuCalculator.PaljaPillar p) {
        return new PaljaResponse.Pillar(p.cheongan(), p.jiji());
    }
}
