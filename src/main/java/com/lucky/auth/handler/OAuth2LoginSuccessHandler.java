package com.lucky.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(@Value("${app.frontend.url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 로그인 성공 → 세션 쿠키(JSESSIONID)는 이미 설정됨. 분리된 프론트엔드로 리다이렉트한다.
        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }
}
