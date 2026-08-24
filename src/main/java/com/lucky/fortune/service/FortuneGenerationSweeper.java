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
    private final FortuneResultService resultService;

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

    /**
     * 제공 기간(결제일 + 1년)이 끝난 리포트의 본문·입력값 파기.
     * 환불정책 제8조·개인정보처리방침 3-1 의 "지체 없이 파기"를 이행하는 자리다.
     *
     * <p>열람 차단은 조회 시점에 이미 걸리므로 이 배치가 늦어도 손님에게 새어 나가지 않는다.
     * 6시간마다면 충분하고, 굳이 자주 돌려 DB 를 훑을 이유가 없다.
     */
    @Scheduled(initialDelay = 120_000, fixedDelay = 21_600_000)
    public void purgeExpired() {
        try {
            int n = resultService.purgeExpired();
            if (n > 0) {
                log.info("[제공기간 만료] {}건의 본문·입력값을 파기했습니다.", n);
            }
        } catch (Exception e) {
            log.error("[제공기간 만료] 파기 실패", e);
        }
    }
}
