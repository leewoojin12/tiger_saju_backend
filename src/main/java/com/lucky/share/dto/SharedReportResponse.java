package com.lucky.share.dto;

import java.time.OffsetDateTime;
import tools.jackson.databind.JsonNode;

/**
 * 공유 링크로 열었을 때 내려주는 리포트. 로그인 없이 읽는 화면이라
 * 리포트 id·회원 id 같은 내부 식별자는 절대 싣지 않는다(주소만 바꿔 남의 것을 넘겨보지 못하게).
 *
 * @param slug      어떤 컨텐츠인지. 프론트가 공개 카탈로그(/api/fortunes/{slug})에서 제목·아이콘을 채운다
 * @param result    풀 리포트 본문 JSON
 * @param input     생성에 쓰인 입력(이름·생년월일시). 리포트 하단 '입력한 정보' 표시용
 * @param expiresAt 링크 만료 시각. 화면에 "이 링크는 …까지 볼 수 있어요"로 안내한다
 */
public record SharedReportResponse(String slug, JsonNode result, JsonNode input,
                                   OffsetDateTime expiresAt) {}
