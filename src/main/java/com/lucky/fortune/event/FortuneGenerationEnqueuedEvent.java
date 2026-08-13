package com.lucky.fortune.event;

/**
 * 풀 리포트 생성 요청이 GENERATING 행으로 적재됐음을 알리는 이벤트.
 * 워커는 트랜잭션 커밋 후(AFTER_COMMIT)에만 이 행을 읽어야 하므로, enqueue 트랜잭션 안에서 발행한다.
 */
public record FortuneGenerationEnqueuedEvent(Long resultId) {
}
