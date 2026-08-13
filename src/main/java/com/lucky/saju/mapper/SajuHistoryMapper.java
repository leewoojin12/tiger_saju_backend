package com.lucky.saju.mapper;

import com.lucky.saju.domain.SajuHistory;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SajuHistoryMapper {

    void insert(SajuHistory history);

    /** 특정 회원의 보관함 목록(최신순). 미리보기 컬럼만 조회. */
    List<SajuHistory> findByMemberId(@Param("memberId") Long memberId);

    /** 본인 소유 1건 상세. 소유자 검증 위해 memberId 조건 포함. */
    Optional<SajuHistory> findByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);
}
