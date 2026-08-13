package com.lucky.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 풀 리포트 비동기 생성용 바운디드 스레드풀.
 * 전역 @Async 기본 풀을 쓰면 동시 결제 시 스레드·OpenAI 레이트가 폭발하므로 별도 풀로 제한한다.
 * (core 2 / max 4 / queue 50). 큐까지 차면 CallerRuns 로 흘려보내 작업 유실은 막는다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("fortuneGenExecutor")
    public Executor fortuneGenExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("fortune-gen-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
