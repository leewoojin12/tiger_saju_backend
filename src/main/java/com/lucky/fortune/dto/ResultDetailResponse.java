package com.lucky.fortune.dto;

import tools.jackson.databind.JsonNode;

/**
 * 보관함 상세(GET /api/results/{id}) 응답. 폴링 대응 봉투(envelope).
 *  - status = DONE      → result 채워짐(기존 리포트 JSON), error=null
 *  - status = GENERATING→ result=null, error=null (프론트: 작성 중… + 3초 폴링)
 *  - status = FAILED    → result=null, error 채워짐(프론트: retryable 이면 재시도 버튼)
 *  - status = REVOKED   → 환불된 결제건. result=null, error=안내 문구, retryable=false
 * null 필드는 직렬화에서 빠지도록 처리하지 않고 그대로 내보낸다(프론트가 status 로만 분기).
 *
 * @param retryable 재시도 요청을 받아줄 수 있는 상태인지(FAILED + 시도 한도 미도달).
 *                  false 인데 FAILED 면 자동 환불되었거나 환불 처리가 필요한 건이다.
 */
public record ResultDetailResponse(Long id, String status, JsonNode result, String error,
                                   boolean retryable) {
}
