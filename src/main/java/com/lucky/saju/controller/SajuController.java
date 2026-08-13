package com.lucky.saju.controller;

import com.lucky.member.domain.Member;
import com.lucky.member.service.MemberService;
import com.lucky.saju.dto.SajuHistoryDetail;
import com.lucky.saju.dto.SajuHistoryListItem;
import com.lucky.saju.dto.SajuRequest;
import com.lucky.saju.dto.SajuResponse;
import com.lucky.saju.service.SajuHistoryService;
import com.lucky.saju.service.SajuService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/saju")
@RequiredArgsConstructor
public class SajuController {

    private final SajuService sajuService;
    private final SajuHistoryService sajuHistoryService;
    private final MemberService memberService;
    private final com.lucky.saju.service.PaljaService paljaService;

    /** 사주 풀이. 성공 시 결과를 보관함에 자동 저장(저장 실패해도 결과는 정상 반환). */
    @PostMapping
    public SajuResponse interpret(@Valid @RequestBody SajuRequest request,
                                  @AuthenticationPrincipal OAuth2User principal) {
        SajuResponse response = sajuService.interpret(request);
        try {
            Member me = currentMember(principal);
            sajuHistoryService.save(request, response, me.getId(), me.getNickname());
        } catch (Exception e) {
            // 저장 실패는 사용자 응답을 막지 않는다(AI 호출 비용 보호). 로그만 남김.
            log.error("사주 결과 보관함 저장 실패", e);
        }
        return response;
    }

    /**
     * 팔자판 전용(경량). AI·보관함 저장 없이 사주팔자 8글자만 계산해 반환.
     * 화면에서 팔자판만 그릴 때 사용(로그인 불필요).
     */
    @PostMapping("/palja")
    public com.lucky.saju.dto.PaljaResponse palja(@Valid @RequestBody SajuRequest request) {
        return paljaService.compute(request);
    }

    /** 보관함 목록(본인 것, 최신순). */
    @GetMapping("/history")
    public List<SajuHistoryListItem> history(@AuthenticationPrincipal OAuth2User principal) {
        return sajuHistoryService.list(currentMember(principal).getId());
    }

    /** 보관함 상세 1건(본인 것). */
    @GetMapping("/history/{id}")
    public SajuHistoryDetail historyDetail(@PathVariable Long id,
                                           @AuthenticationPrincipal OAuth2User principal) {
        return sajuHistoryService.detail(id, currentMember(principal).getId());
    }

    /** 로그인한 회원 조회. principal 의 카카오 id 로 member 를 찾는다. */
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
