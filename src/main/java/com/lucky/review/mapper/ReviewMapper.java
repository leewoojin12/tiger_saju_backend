package com.lucky.review.mapper;

import com.lucky.review.dto.ReviewPublic;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewMapper {

    /** 공개(PUBLIC) 후기만 최신순. slug 을 주면 그 상품 것만. */
    List<ReviewPublic> findPublic(@Param("slug") String slug, @Param("limit") int limit);

    /** 공개된 후기 개수(홈에서 섹션을 띄울지 판단). */
    int countPublic(@Param("slug") String slug);
}
