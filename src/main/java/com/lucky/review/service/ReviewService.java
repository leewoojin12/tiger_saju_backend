package com.lucky.review.service;

import com.lucky.review.dto.ReviewPublic;
import com.lucky.review.mapper.ReviewMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    /** 한 번에 내보내는 최대 개수. 홈 후기 띠는 이보다 훨씬 적게 쓴다. */
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_LIMIT = 30;

    private final ReviewMapper reviewMapper;

    /**
     * 공개된 후기 목록. 검토를 통과한(PUBLIC) 글만 나간다.
     * 아직 후기가 없으면 빈 목록 → 화면에서 후기 영역을 통째로 감춘다.
     */
    @Transactional(readOnly = true)
    public List<ReviewPublic> listPublic(String slug, Integer limit) {
        int n = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        String s = slug == null || slug.isBlank() ? null : slug;
        return reviewMapper.findPublic(s, n);
    }
}
