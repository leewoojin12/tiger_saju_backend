package com.lucky.member.mapper;

import com.lucky.member.domain.Member;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper {

    Optional<Member> findByKakaoId(Long kakaoId);

    void insert(Member member);

    void updateNickname(Member member);
}
