package com.lucky.review.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportRatingMapper {

    /**
     * 별점 저장. 이미 남긴 리포트면 UNIQUE 제약에 걸리므로 ON CONFLICT DO NOTHING 으로
     * 0 을 반환한다(덮어쓰지 않는다 = 1회 제한).
     */
    int insertIfAbsent(@Param("resultId") Long resultId,
                       @Param("memberId") Long memberId,
                       @Param("slug") String slug,
                       @Param("rating") int rating);

    /** 이 리포트에 남긴 별점. 없으면 null. */
    Integer findByResultId(@Param("resultId") Long resultId);
}
