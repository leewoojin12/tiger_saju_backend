package com.lucky.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 심사용 테스트 로그인 설정(app.test-login.*).
 *
 * <p>이 서비스는 카카오 로그인만 제공하는데, 결제사·스토어 심사에서는
 * "테스트 아이디와 비밀번호"를 요구한다. 심사 기간에만 켜두는 임시 통로다.
 *
 * <p>기본값은 꺼짐(enabled=false)이고 비밀번호는 환경변수로만 넣는다.
 * 심사가 끝나면 TEST_LOGIN_ENABLED 를 내리는 것만으로 통로가 닫힌다.
 *
 * @param enabled  꺼져 있으면 엔드포인트가 404 로 응답한다(존재 자체를 알리지 않는다)
 * @param email    심사기관에 알려줄 아이디
 * @param password 심사기관에 알려줄 비밀번호. 반드시 환경변수(TEST_LOGIN_PASSWORD)로 주입한다
 * @param kakaoId  이 계정이 쓸 members.kakao_id. 실제 카카오 id 와 겹치지 않게 음수를 쓴다
 * @param nickname 화면에 표시될 이름
 */
@ConfigurationProperties(prefix = "app.test-login")
public record TestLoginProperties(
        boolean enabled,
        String email,
        String password,
        Long kakaoId,
        String nickname
) {
}
