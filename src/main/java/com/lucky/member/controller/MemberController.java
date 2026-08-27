package com.lucky.member.controller;

import com.lucky.member.service.MemberService;
import com.lucky.member.service.MemberWithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원 계정 API. 지금은 탈퇴 하나뿐이다.
 * 이용약관이 "서비스 내 기능 또는 고객문의 이메일을 통해 탈퇴를 신청할 수 있다"고 밝히고 있어,
 * 화면에서 바로 처리할 수 있는 경로가 있어야 한다.
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MemberWithdrawalService withdrawalService;

    /** 탈퇴. 되돌릴 수 없다. 처리 후 세션을 끊어 즉시 로그아웃 상태가 된다. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal OAuth2User principal,
                         HttpServletRequest request) {
        withdrawalService.withdraw(currentMemberId(principal));

        // 남은 세션으로는 이미 파기된 회원을 계속 들고 다니게 된다. 여기서 끊는다.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
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
