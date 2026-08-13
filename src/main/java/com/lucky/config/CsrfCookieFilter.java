package com.lucky.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 지연 로딩되는 CSRF 토큰을 매 요청마다 강제로 로드시켜,
 * 응답에 XSRF-TOKEN 쿠키가 실제로 내려가도록 한다(프론트가 읽을 수 있게).
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();   // 토큰 로드 → 쿠키 기록 유발
        }
        filterChain.doFilter(request, response);
    }
}
