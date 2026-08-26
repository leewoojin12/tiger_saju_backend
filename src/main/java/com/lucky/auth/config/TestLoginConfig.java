package com.lucky.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 심사용 테스트 로그인 설정 등록.
 * 심사가 끝나 이 기능을 걷어낼 때 auth/config·auth/dto·TestLoginController 만 지우면 된다.
 */
@Configuration
@EnableConfigurationProperties(TestLoginProperties.class)
public class TestLoginConfig {
}
