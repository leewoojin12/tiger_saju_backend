package com.lucky.fortune.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * 생성(맛보기/풀) 공통 입력.
 *  - subjects : 구조화된 사람들(사주 계산 대상). 1명(연애운)~2명(궁합)~N명.
 *  - answers  : 컨텐츠별 질문지 응답 등 자유형. 백엔드는 계산에 쓰지 않고 AI 컨텍스트로만 전달.
 */
public record GenerateRequest(
        @NotEmpty @Valid List<Subject> subjects,
        Map<String, Object> answers
) {
}
