package com.lucky.saju.service;

import com.lucky.saju.domain.SajuHistory;
import com.lucky.saju.dto.SajuHistoryDetail;
import com.lucky.saju.dto.SajuHistoryListItem;
import com.lucky.saju.dto.SajuRequest;
import com.lucky.saju.dto.SajuResponse;
import com.lucky.saju.mapper.SajuHistoryMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** 사주 보관함: 저장 / 목록 / 상세. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SajuHistoryService {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SajuHistoryMapper sajuHistoryMapper;

    /** 사주 풀이 결과를 보관함에 저장하고 저장된 id 를 반환. */
    public Long save(SajuRequest req, SajuResponse res, Long memberId, String username) {
        String resultJson;
        try {
            resultJson = JSON.writeValueAsString(res);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결과 직렬화 실패", e);
        }
        SajuHistory history = SajuHistory.builder()
                .memberId(memberId)
                .username(username)
                .name(req.name())
                .gender(req.gender())
                .calendar(req.calendar())
                .birthDate(req.birthDate())
                .birthTime(req.timeUnknown() ? null : req.birthTime())
                .timeUnknown(req.timeUnknown())
                .iljuName(res.ilju() != null ? res.ilju().name() : null)
                .summary(res.summary())
                .resultJson(resultJson)
                .build();
        sajuHistoryMapper.insert(history);
        return history.getId();
    }

    /** 본인 보관함 목록(최신순). */
    @Transactional(readOnly = true)
    public List<SajuHistoryListItem> list(Long memberId) {
        return sajuHistoryMapper.findByMemberId(memberId).stream()
                .map(h -> new SajuHistoryListItem(
                        h.getId(), h.getName(), h.getIljuName(), h.getSummary(), h.getCreatedAt()))
                .toList();
    }

    /** 본인 보관함 상세 1건. 없거나 남의 것이면 404. */
    @Transactional(readOnly = true)
    public SajuHistoryDetail detail(Long id, Long memberId) {
        SajuHistory h = sajuHistoryMapper.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보관함에서 찾을 수 없습니다."));
        SajuResponse result;
        try {
            result = JSON.readValue(h.getResultJson(), SajuResponse.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결과 역직렬화 실패", e);
        }
        return new SajuHistoryDetail(
                h.getId(), h.getName(), h.getGender(), h.getCalendar(),
                h.getBirthDate(), h.getBirthTime(), h.isTimeUnknown(), h.getCreatedAt(), result);
    }
}
