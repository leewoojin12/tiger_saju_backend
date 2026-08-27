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

    /**
     * 탈퇴: 이 회원의 옛 보관함(saju_history) 삭제.
     * 이름·생년월일·시각이 컬럼에 그대로 들어 있고 결제와 무관한 기록이라 행째 지운다.
     */
    int deleteByMemberId(@Param("memberId") Long memberId);
}
