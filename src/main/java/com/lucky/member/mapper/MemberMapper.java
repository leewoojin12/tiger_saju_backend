package com.lucky.member.mapper;

import com.lucky.member.domain.Member;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

    Optional<Member> findByKakaoId(Long kakaoId);

    void insert(Member member);

    void updateNickname(Member member);

    /**
     * 탈퇴 처리. 행은 남기고 식별 정보만 지운다(결제 기록이 member_id 로 걸려 있어 행을 지울 수 없다).
     * kakao_id 는 NOT NULL·UNIQUE 라 비울 수 없으므로 다시는 겹치지 않을 값으로 덮는다.
     */
    int withdraw(@Param("memberId") Long memberId);
}
