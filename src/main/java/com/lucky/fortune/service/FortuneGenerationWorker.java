package com.lucky.fortune.service;

import com.lucky.fortune.event.FortuneGenerationEnqueuedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 풀 리포트 생성 워커.
 *
 * <p>{@code @TransactionalEventListener(AFTER_COMMIT)} : enqueue 트랜잭션이 커밋된 뒤에만 실행 →
 * GENERATING 행이 DB에 확실히 보이는 시점이라 워커가 읽을 수 있다.
 * <p>{@code @Async("fortuneGenExecutor")} : 별도 바운디드 풀에서 실행 → 요청 스레드(HTTP 202 응답)와 분리.
 * <p>서비스가 워커를 의존하지 않으므로(워커→서비스 단방향) 순환참조 없음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FortuneGenerationWorker {

    private final FortuneGenerationService generationService;

    @Async("fortuneGenExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnqueued(FortuneGenerationEnqueuedEvent event) {
        log.info("풀 리포트 생성 시작 id={}", event.resultId());
        generationService.runGeneration(event.resultId());
    }
}
