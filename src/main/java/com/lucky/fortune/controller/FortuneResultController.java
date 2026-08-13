package com.lucky.fortune.controller;

import com.lucky.fortune.dto.EnqueueResponse;
import com.lucky.fortune.dto.MyResultSummary;
import com.lucky.fortune.dto.ResultDetailResponse;
import com.lucky.fortune.service.FortuneGenerationService;
import com.lucky.fortune.service.FortuneResultService;
import com.lucky.member.service.MemberService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 보관함 API (로그인 필요). "/api/results" 는 카탈로그 GET permitAll('/api/fortunes', '/api/fortunes/*')에
 * 안 걸리므로 anyRequest().authenticated() 로 인증이 강제된다.
 */
@RestController
@RequestMapping("/api/results")
public class FortuneResultController {

    private final FortuneResultService resultService;
    private final FortuneGenerationService generationService;
    private final MemberService memberService;

    public FortuneResultController(FortuneResultService resultService,
                                   FortuneGenerationService generationService,
                                   MemberService memberService) {
        this.resultService = resultService;
        this.generationService = generationService;
        this.memberService = memberService;
    }

    /** 내가 결제·생성한 풀 리포트 목록(최신순). */
    @GetMapping
    public List<MyResultSummary> list(@AuthenticationPrincipal OAuth2User principal) {
        return resultService.listMine(currentMemberId(principal));
    }

    /**
     * 저장된 풀 리포트 상세(폴링용). status 봉투 반환:
     * GENERATING(작성 중) / DONE(result 채움) / FAILED(error 채움). 본인 것만.
     */
    @GetMapping("/{id}")
    public ResultDetailResponse detail(@PathVariable Long id,
                                       @AuthenticationPrincipal OAuth2User principal) {
        return resultService.getMine(id, currentMemberId(principal));
    }

    /** FAILED 풀 리포트 재시도. 본인 것만. 즉시 {id, status=GENERATING} 반환 → 다시 폴링. */
    @PostMapping("/{id}/retry")
    public EnqueueResponse retry(@PathVariable Long id,
                                 @AuthenticationPrincipal OAuth2User principal) {
        return generationService.retry(id, currentMemberId(principal));
    }

    private Long currentMemberId(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Object id = principal.getAttribute("id");
        if (!(id instanceof Number kakaoId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return memberService.getByKakaoId(kakaoId.longValue()).getId();
    }
}
