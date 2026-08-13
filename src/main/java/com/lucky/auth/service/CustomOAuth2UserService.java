package com.lucky.auth.service;

import com.lucky.member.domain.Member;
import com.lucky.member.service.MemberService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        Long kakaoId = ((Number) attributes.get("id")).longValue();
        String nickname = extractNickname(attributes);

        Member member = memberService.upsertByKakao(kakaoId, nickname);

        // user-name-attribute("id")는 application.yaml 설정과 반드시 일치해야 한다.
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(member.getRole())),
                attributes,
                "id"
        );
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
