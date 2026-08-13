package com.lucky.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * 분리된 SPA용 CSRF 요청 핸들러 (Spring Security 공식 패턴).
 * - 응답에 토큰을 실을 때는 XOR 인코딩(BREACH 공격 방지).
 * - 프론트가 쿠키(XSRF-TOKEN)에서 읽은 raw 토큰을 X-XSRF-TOKEN 헤더로 보낼 때는 평문으로 검증.
 */
final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // 토큰을 응답으로 내릴 때는 항상 XOR 처리
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // 헤더로 온 경우(SPA가 쿠키 값을 그대로 echo): 평문으로 검증
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        // 그 외(폼 파라미터 _csrf 등): XOR 처리
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
