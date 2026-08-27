package com.lucky.share.mapper;

import com.lucky.share.dto.SharedRow;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportShareMapper {

    /** 공유 링크 한 건 발급. 토큰은 해시로만 저장한다. */
    int insert(@Param("tokenHash") String tokenHash,
               @Param("resultId") Long resultId,
               @Param("memberId") Long memberId,
               @Param("expiresAt") OffsetDateTime expiresAt);

    /**
     * 토큰으로 리포트 찾기. 만료·해제·삭제·환불 판단에 필요한 값을 한 번에 가져온다.
     * (조건을 SQL 에 흩어 놓지 않고 서비스에서 한자리에 모아 판단한다)
     */
    Optional<SharedRow> findByTokenHash(@Param("tokenHash") String tokenHash);
}
