package com.lucky.auth.controller;

import com.lucky.auth.config.TestLoginProperties;
import com.lucky.auth.dto.TestLoginRequest;
import com.lucky.member.domain.Member;
import com.lucky.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 심사용 테스트 로그인. 결제사·스토어 심사에서 "테스트 아이디/비밀번호"를 요구하는데
 * 이 서비스는 카카오 로그인만 있어서, 심사 기간에만 열어두는 임시 통로다.
 *
 * <p>로그인이 성공하면 카카오 로그인과 <b>똑같은 모양의 세션</b>을 만든다.
 * 보관함·결제 등 나머지 코드는 principal 의 "id"(=members.kakao_id)만 보기 때문에,
 * 이 한 곳만 맞춰두면 다른 코드는 손댈 필요가 없다.
 *
 * <p>심사가 끝나면 TEST_LOGIN_ENABLED=false 로 내리면 통로가 닫힌다(엔드포인트가 404).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TestLoginController {

    /** 연속 실패 허용치와 잠금 시간. 계정이 하나뿐이라 전역 카운터로 충분하다. */
    private static final int MAX_FAILS = 10;
    private static final Duration LOCK_FOR = Duration.ofMinutes(10);

    private final TestLoginProperties props;
    private final MemberService memberService;

    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final AtomicInteger fails = new AtomicInteger();
    private volatile Instant lockedUntil = Instant.EPOCH;

    /**
     * 실패도 본문에 message 를 담아 돌려준다. 이 서비스는 에러 메시지 노출이 꺼져 있어
     * (server.error.include-message 기본값) 예외를 던지면 프론트에 "Unauthorized" 만 닿는다.
     * 심사원이 보는 화면이라 무슨 일인지는 알려줘야 한다.
     */
    @PostMapping("/test-login")
    public ResponseEntity<Map<String, Object>> testLogin(@RequestBody TestLoginRequest request,
                                                         HttpServletRequest httpRequest,
                                                         HttpServletResponse httpResponse) {
        // 꺼져 있으면 '있는데 막혔다'는 사실조차 알리지 않는다.
        if (!props.enabled() || isBlank(props.password()) || isBlank(props.email())) {
            return error(HttpStatus.NOT_FOUND, "지원하지 않는 로그인 방식입니다.");
        }
        if (Instant.now().isBefore(lockedUntil)) {
            return error(HttpStatus.TOO_MANY_REQUESTS,
                    "로그인 시도가 너무 많아요. 10분 뒤에 다시 시도해 주세요.");
        }
        // 이메일·비밀번호 모두 상수 시간 비교(응답 시간으로 정답을 좁히지 못하게).
        // & 를 쓰는 이유: && 는 앞이 틀리면 뒤를 건너뛰어 비교 횟수가 달라진다.
        boolean ok = constantTimeEquals(props.email(), request.email())
                & constantTimeEquals(props.password(), request.password());
        if (!ok) {
            if (fails.incrementAndGet() >= MAX_FAILS) {
                lockedUntil = Instant.now().plus(LOCK_FOR);
                fails.set(0);
            }
            return error(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        fails.set(0);

        String nickname = isBlank(props.nickname()) ? "테스트 계정" : props.nickname();
        Member member = memberService.upsertByKakao(props.kakaoId(), nickname);
        authenticate(member, nickname, httpRequest, httpResponse);

        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("kakaoId", props.kakaoId());
        result.put("nickname", nickname);
        result.put("isAdmin", false);
        return ResponseEntity.ok(result);
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("authenticated", false);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * 카카오 로그인이 만드는 것과 같은 principal 을 세션에 심는다.
     * attributes 모양(id · kakao_account.profile.nickname)까지 맞춰야
     * /api/auth/me 가 닉네임을 그대로 읽어간다.
     */
    private void authenticate(Member member, String nickname,
                              HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Map<String, Object> attributes = Map.of(
                "id", props.kakaoId(),
                "kakao_account", Map.of("profile", Map.of("nickname", nickname)));
        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority(member.getRole())), attributes, "id");
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        // 세션 고정 공격 방지: 로그인 전 세션은 버리고 새로 발급한다.
        // CSRF 토큰은 세션이 아니라 쿠키에 있으므로 여기서 무효화해도 영향이 없다.
        HttpSession existing = httpRequest.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        httpRequest.getSession(true);
        contextRepository.saveContext(context, httpRequest, httpResponse);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 길이·내용이 응답 시간에 드러나지 않게 비교한다. */
    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
