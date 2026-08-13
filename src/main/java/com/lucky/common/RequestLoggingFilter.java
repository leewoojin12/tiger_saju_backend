package com.lucky.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 들어온 요청을 보기 좋은 블록으로 로그에 남긴다.
 *   메서드/URI/쿼리 · 파라미터 · 바디 · 응답상태 · 소요시간
 *
 * 주의: 사주 입력(이름·생년월일 등 개인정보)이 바디에 담기므로 운영 로그 파일에도 그대로 남는다(취급 주의).
 * 보안 필터보다 먼저 돌도록 HIGHEST_PRECEDENCE → 403/401로 막힌 요청도 로그에 남는다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final int MAX_BODY = 2000;   // 바디 로그 최대 길이(초과 시 잘림)

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 에러 디스패치는 중복이라 스킵
        if ("/error".equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, MAX_BODY);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            logRequest(wrapped, response, System.currentTimeMillis() - start);
        }
    }

    private void logRequest(ContentCachingRequestWrapper req, HttpServletResponse res, long tookMs) {
        String query = req.getQueryString();
        String params = req.getParameterMap().entrySet().stream()
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining(", "));
        // 바디는 컨트롤러가 읽은 경우에만 캐시됨(미인증/CSRF 차단 등으로 안 읽히면 빈 값)
        String body = new String(req.getContentAsByteArray(), StandardCharsets.UTF_8);
        if (body.length() > MAX_BODY) {
            body = body.substring(0, MAX_BODY) + "...(truncated)";
        }

        StringBuilder sb = new StringBuilder("\n");
        sb.append("============================================\n");
        sb.append("[API  ] ").append(req.getMethod()).append(' ').append(req.getRequestURI());
        if (query != null) {
            sb.append('?').append(query);
        }
        sb.append('\n');
        sb.append("[FROM ] ip=").append(clientIp(req)).append("  user=").append(currentUser(req)).append('\n');
        sb.append("[PARAM] ").append(params.isEmpty() ? "-" : params).append('\n');
        if (!body.isBlank()) {
            sb.append("[BODY ] ").append(body).append('\n');
        }
        sb.append("[RESP ] ").append(res.getStatus()).append("  (").append(tookMs).append("ms)\n");
        sb.append("============================================");
        log.info(sb.toString());
    }

    /** 클라이언트 IP. nginx 뒤이므로 X-Forwarded-For 첫 IP 우선, 없으면 remoteAddr. */
    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > -1 ? xff.substring(0, comma) : xff).trim();
        }
        String remote = req.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "?" : remote;
    }

    /**
     * 요청자 카카오 ID. 이 필터는 보안 필터보다 바깥이라 로그 시점(finally)엔 ThreadLocal 컨텍스트가
     * 비워졌을 수 있으므로, 없으면 세션에 저장된 SecurityContext 에서 읽는다. 비로그인은 'anonymous'.
     */
    private String currentUser(HttpServletRequest req) {
        String fromHolder = kakaoIdOf(SecurityContextHolder.getContext().getAuthentication());
        if (fromHolder != null) {
            return fromHolder;
        }
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object ctx = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (ctx instanceof SecurityContext sc) {
                String fromSession = kakaoIdOf(sc.getAuthentication());
                if (fromSession != null) {
                    return fromSession;
                }
            }
        }
        return "anonymous";
    }

    /** Authentication 의 principal(OAuth2User)에서 카카오 id 추출. 익명/미인증이면 null. */
    private String kakaoIdOf(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof OAuth2User user) {
            Object id = user.getAttribute("id");
            return id != null ? String.valueOf(id) : null;
        }
        return null;
    }
}
