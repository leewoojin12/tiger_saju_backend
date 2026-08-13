package com.lucky.fortune.controller;

import com.lucky.fortune.dto.FortunePublic;
import com.lucky.fortune.service.FortuneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공개 카탈로그 API. 로그인 없이 GET 가능 (가격/문구는 공개 정보).
 * 응답은 항상 {@link FortunePublic} → 프롬프트는 절대 노출되지 않음.
 *
 * <p>주의: 맛보기/풀생성(POST)은 더 깊은 경로(/api/fortunes/&#123;slug&#125;/teaser 등)에 두어
 * 인증이 걸리도록 한다. SecurityConfig 에서 GET /api/fortunes, /api/fortunes/* 만 permitAll.
 */
@RestController
@RequestMapping("/api/fortunes")
public class FortuneController {

    private final FortuneService fortuneService;

    public FortuneController(FortuneService fortuneService) {
        this.fortuneService = fortuneService;
    }

    /** 노출 중인 운세 목록. */
    @GetMapping
    public List<FortunePublic> list() {
        return fortuneService.listActive();
    }

    /** 추천(인기) 탑5 — 메인 '추천 사주' 레일. ('/{slug}' 보다 구체적 경로라 우선 매칭) */
    @GetMapping("/popular")
    public List<FortunePublic> popular() {
        return fortuneService.listPopular();
    }

    /** 운세 상세 — 랜딩 페이지가 제목/설명/가격/소요시간을 여기서 받음. */
    @GetMapping("/{slug}")
    public FortunePublic detail(@PathVariable String slug) {
        return fortuneService.getPublic(slug);
    }
}
