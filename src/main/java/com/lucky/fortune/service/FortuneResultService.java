package com.lucky.fortune.service;

import com.lucky.fortune.domain.FortuneResult;
import com.lucky.fortune.dto.MyResultSummary;
import com.lucky.fortune.dto.ResultDetailResponse;
import com.lucky.fortune.mapper.FortuneResultMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 보관함: 결제 완료 후 저장된 풀 리포트 재조회.
 * 생성/저장은 {@link FortuneGenerationService#generateFull}이 담당하고, 여기선 읽기만.
 */
@Service
@RequiredArgsConstructor
public class FortuneResultService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final FortuneResultMapper resultMapper;

    /** 내 보관함 목록 (최신순, 본문 제외). */
    public List<MyResultSummary> listMine(Long memberId) {
        return resultMapper.findSummariesByMemberId(memberId);
    }

    /**
     * 보관함 상세(폴링 대응). status 봉투로 반환:
     *  - DONE       → result(본문 JSON) 채움
     *  - GENERATING → result/error 없음 (프론트: 작성 중… + 3초 폴링)
     *  - FAILED     → error 채움 (프론트: 재시도 버튼)
     * 본인 것만.
     */
    public ResultDetailResponse getMine(Long id, Long memberId) {
        FortuneResult result = resultMapper.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "결과를 찾을 수 없습니다."));
        String status = result.getStatus();
        if ("DONE".equals(status)) {
            JsonNode tree;
            try {
                tree = JSON.readTree(result.getResultJson());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "저장된 결과를 읽을 수 없습니다.");
            }
            return new ResultDetailResponse(id, "DONE", tree, null);
        }
        if ("FAILED".equals(status)) {
            return new ResultDetailResponse(id, "FAILED", null, result.getError());
        }
        return new ResultDetailResponse(id, "GENERATING", null, null);
    }
}
