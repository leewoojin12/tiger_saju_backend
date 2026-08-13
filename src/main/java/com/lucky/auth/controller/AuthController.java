package com.lucky.auth.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * 현재 로그인 사용자 정보. (/api/auth/me 는 permitAll 이라 미인증이면 principal 이 null)
     * 분리된 프론트엔드가 로그인 상태 확인용으로 호출한다.
     */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> result = new HashMap<>();
        if (principal == null) {
            result.put("authenticated", false);
            return result;
        }
        Map<String, Object> attributes = principal.getAttributes();
        result.put("authenticated", true);
        result.put("kakaoId", attributes.get("id"));
        result.put("nickname", extractNickname(attributes));
        result.put("isAdmin", principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
        return result;
    }

    @SuppressWarnings("unchecked")
    private String extractNickname(Map<String, Object> attributes) {
        Object kakaoAccount = attributes.get("kakao_account");
        if (kakaoAccount instanceof Map<?, ?> account) {
            Object profile = ((Map<String, Object>) account).get("profile");
            if (profile instanceof Map<?, ?> p) {
                Object nickname = ((Map<String, Object>) p).get("nickname");
                if (nickname != null) {
                    return nickname.toString();
                }
            }
        }
        return null;
    }
}
