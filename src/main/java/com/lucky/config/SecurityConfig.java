package com.lucky.config;

import com.lucky.auth.handler.OAuth2LoginSuccessHandler;
import com.lucky.auth.service.CustomOAuth2UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Value("${app.cookie.domain:}")
    private String cookieDomain;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 분리된 SPA용 CSRF: 쿠키(XSRF-TOKEN)로 토큰 전달 → 프론트가 X-XSRF-TOKEN 헤더로 echo.
                .csrf(csrf -> csrf
                        // 포트원 웹훅은 외부(포트원 서버)가 호출 → CSRF 토큰 없음. 서명으로 검증하므로 CSRF 제외
                        .ignoringRequestMatchers("/api/payments/webhook")
                        .csrfTokenRepository(csrfTokenRepository())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                // 지연 로딩되는 CSRF 토큰을 강제 로드해 XSRF-TOKEN 쿠키가 응답에 실리도록 함
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/api/auth/me", "/api/payments/webhook").permitAll()
                        // 카탈로그는 공개(가격/문구). GET만 허용 — POST(맛보기/풀생성)는 인증 필요하므로 제외.
                        // "/api/fortunes/*" 는 한 세그먼트만 매칭 → /api/fortunes/{slug}/teaser(두 세그먼트)는 인증 유지.
                        .requestMatchers(HttpMethod.GET, "/api/fortunes", "/api/fortunes/*").permitAll()
                        // 팔자판 전용 경량 계산(AI·저장 없음)은 공개 — 비로그인 화면에서도 실제 팔자 표시.
                        .requestMatchers(HttpMethod.POST, "/api/saju/palja").permitAll()
                        // 관리자 전용: members.role = 'ROLE_ADMIN' 인 회원만 (CustomOAuth2UserService가 role을 권한으로 부여)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, authentication) -> res.setStatus(HttpStatus.OK.value()))
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                )
                // 미인증 API 요청은 로그인 페이지로 리다이렉트하지 않고 401을 반환(프론트가 처리).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
        return http.build();
    }

    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        // 프론트(www)와 백엔드(api)가 다른 서브도메인이라, 프론트 JS가 XSRF 쿠키를 읽을 수 있도록
        // 상위 도메인(.trendsaju.com)으로 내려준다. 로컬(빈 값)에선 호스트 전용.
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            repo.setCookieCustomizer(c -> c.domain(cookieDomain));
        }
        return repo;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);   // 세션 쿠키(JSESSIONID) 교차 출처 전송 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
