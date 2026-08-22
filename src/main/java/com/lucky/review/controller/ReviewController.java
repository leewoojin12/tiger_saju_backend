package com.lucky.review.controller;

import com.lucky.review.dto.ReviewPublic;
import com.lucky.review.service.ReviewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 후기 API. 로그인 없이 GET 가능(홈 상단 후기 띠가 비로그인 상태에서도 보여야 한다).
 * 검토를 통과한 PUBLIC 글만 나가며, 회원 id 같은 내부 식별자는 응답에 포함하지 않는다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public List<ReviewPublic> list(@RequestParam(required = false) String slug,
                                   @RequestParam(required = false) Integer limit) {
        return reviewService.listPublic(slug, limit);
    }
}
