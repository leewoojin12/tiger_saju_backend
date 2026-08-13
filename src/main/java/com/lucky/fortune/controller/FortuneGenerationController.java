package com.lucky.fortune.controller;

import com.lucky.fortune.dto.EnqueueResponse;
import com.lucky.fortune.dto.FullReportRequest;
import com.lucky.fortune.dto.GenerateRequest;
import com.lucky.fortune.service.FortuneGenerationService;
import com.lucky.member.domain.Member;
import com.lucky.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * 운세 생성 API (로그인 필요). /api/fortunes/{slug} 보다 깊은 경로라
 * SecurityConfig의 GET-only permitAll 에 안 걸림 → 인증 강제됨. (POST → CSRF 필요)
 */
@RestController
@RequestMapping("/api/fortunes")
public class FortuneGenerationController {

    private final FortuneGenerationService generationService;
    private final MemberService memberService;

    public FortuneGenerationController(FortuneGenerationService generationService,
                                       MemberService memberService) {
        this.generationService = generationService;
        this.memberService = memberService;
    }

    /**
     * 무료 맛보기. 결정적 facts + 도입부(짧은 AI)를 즉시 반환(저장 안 함).
     * 응답: AI출력(headline/intro 등) ∪ facts(subjects/pair). 형태는 운세별 프롬프트가 정의.
     */
    @PostMapping("/{slug}/teaser")
    public JsonNode teaser(@PathVariable String slug,
                           @Valid @RequestBody GenerateRequest req,
                           @AuthenticationPrincipal OAuth2User principal) {
        currentMember(principal);   // 로그인 강제
        return generationService.generateTeaser(slug, req);
    }

    /**
     * 유료 풀 리포트 <b>생성 요청</b>. 결제(PAID) 검증 후 즉시 202 + {id, status} 반환,
     * 실제 생성은 백그라운드에서 진행(폴링). 프론트는 받은 id 로 보관함 상세를 3초 폴링한다.
     * 결제 1건당 1회(멱등) — 재요청·더블탭이면 기존 행 상태를 그대로 돌려줌.
     */
    @PostMapping("/{slug}/full")
    public ResponseEntity<EnqueueResponse> full(@PathVariable String slug,
                                                @Valid @RequestBody FullReportRequest req,
                                                @AuthenticationPrincipal OAuth2User principal) {
        Long memberId = currentMember(principal).getId();
        EnqueueResponse res =
                generationService.enqueueFull(slug, req.paymentId(), req.input(), req.intro(), memberId);
        return ResponseEntity.accepted().body(res);   // 202 Accepted
    }

    /** 로그인한 회원 조회 (카카오 id → member). */
    private Member currentMember(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Object id = principal.getAttribute("id");
        if (!(id instanceof Number kakaoId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return memberService.getByKakaoId(kakaoId.longValue());
    }
}
