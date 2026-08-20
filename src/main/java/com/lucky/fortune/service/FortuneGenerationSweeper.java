package com.lucky.fortune.service;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 좀비 생성건 정리 스케줄러.
 *
 * <p>배포(EC2 재시작)나 장애로 워커 스레드가 죽으면 fortune_results 행이 GENERATING 인 채 영원히 남는다.
 * 사용자 화면은 3초 폴링이라 무한 로딩이 되고, 결제는 이미 끝난 상태다.
 * 일정 시간이 지난 GENERATING 행을 FAILED 로 마감해 재시도 버튼이 뜨게 하고,
 * 시도 한도에 도달했으면 자동 환불까지 이어지게 한다.
 *
 * <p>AI 호출 타임아웃이 120초이므로 정상 생성이 STUCK_MINUTES 를 넘길 일은 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FortuneGenerationSweeper {

    /** 이 시간(분)보다 오래 GENERATING 이면 죽은 것으로 본다. */
    private static final int STUCK_MINUTES = 15;

    private final FortuneGenerationService generationService;

    /** 5분마다. 애플리케이션 기동 1분 뒤부터. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 300_000)
    public void sweep() {
        try {
            int n = generationService.failStuckGenerating(
                    OffsetDateTime.now().minusMinutes(STUCK_MINUTES));
            if (n > 0) {
                log.warn("[좀비 정리] {}건을 FAILED 로 마감했습니다.", n);
            }
        } catch (Exception e) {
            log.error("[좀비 정리] 실행 실패", e);
        }
    }
}
