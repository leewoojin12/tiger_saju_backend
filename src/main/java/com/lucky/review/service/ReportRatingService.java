package com.lucky.review.service;

import com.lucky.fortune.domain.FortuneResult;
import com.lucky.fortune.mapper.FortuneResultMapper;
import com.lucky.review.mapper.ReportRatingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 리포트 별점(★1~5). 리포트 1건당 한 번만 남길 수 있고 수정·취소는 없다.
 * 후기(reviews)와 달리 검토 절차가 없으므로 바로 저장된다.
 */
@Service
@RequiredArgsConstructor
public class ReportRatingService {

    private final ReportRatingMapper ratingMapper;
    private final FortuneResultMapper resultMapper;

    /**
     * 별점 남기기. 본인이 결제해 받은 리포트만.
     *
     * <p>이미 남긴 경우를 에러로 만들지 않는다. 화면은 이미 잠겨 있어야 정상이고,
     * 뒤늦게 도착한 중복 요청 때문에 사용자가 빨간 메시지를 볼 이유는 없다.
     * 대신 항상 '현재 저장된 점수'를 돌려주어 화면이 그 값으로 잠기게 한다.
     *
     * @return 저장(또는 이미 저장돼 있던) 별점
     */
    @Transactional
    public int rate(Long resultId, Long memberId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "별점은 1~5 사이여야 합니다.");
        }
        FortuneResult result = resultMapper.findByIdAndMemberId(resultId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "결과를 찾을 수 없습니다."));
        // 아직 글이 만들어지지 않았거나 환불된 건은 평가 대상이 아니다.
        if (!"DONE".equals(result.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "완성된 리포트만 평가할 수 있어요.");
        }
        ratingMapper.insertIfAbsent(resultId, memberId, result.getSlug(), rating);
        Integer saved = ratingMapper.findByResultId(resultId);
        return saved != null ? saved : rating;
    }

    /** 이 리포트에 남긴 별점(없으면 null). 상세 응답에 실어 화면을 잠그는 데 쓴다. */
    @Transactional(readOnly = true)
    public Integer findRating(Long resultId) {
        return ratingMapper.findByResultId(resultId);
    }
}
