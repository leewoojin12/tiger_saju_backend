package com.lucky.fortune.domain;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 결제 완료 후 생성된 풀 리포트 결과(fortune_results 테이블). 결제 1건당 1행. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FortuneResult {

    private Long id;
    private Long memberId;
    private String slug;
    private String paymentId;
    private String name;
    private String resultJson;   // 풀 리포트 AI JSON(원문). 생성 중(GENERATING)이면 NULL.
    private String status;       // GENERATING / DONE / FAILED
    private String error;        // FAILED 시 사용자 안내 메시지
    private String inputJson;    // 재시도용 생성 입력 {"input":..,"intro":..}
    private OffsetDateTime createdAt;
}
