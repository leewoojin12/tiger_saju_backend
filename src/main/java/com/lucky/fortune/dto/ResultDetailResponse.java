package com.lucky.fortune.dto;

import tools.jackson.databind.JsonNode;

/**
 * 보관함 상세(GET /api/results/{id}) 응답. 폴링 대응 봉투(envelope).
 *  - status = DONE      → result 채워짐(기존 리포트 JSON), error=null
 *  - status = GENERATING→ result=null, error=null (프론트: 작성 중… + 3초 폴링)
 *  - status = FAILED    → result=null, error 채워짐(프론트: 재시도 버튼)
 * null 필드는 직렬화에서 빠지도록 처리하지 않고 그대로 내보낸다(프론트가 status 로만 분기).
 */
public record ResultDetailResponse(Long id, String status, JsonNode result, String error) {
}
