package com.lucky.member.service;

import com.lucky.member.domain.Member;
import com.lucky.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;

    /** kakaoId 로 회원 조회(없으면 401). 로그인 사용자의 member_id/닉네임 확보용. */
    @Transactional(readOnly = true)
    public Member getByKakaoId(Long kakaoId) {
        return memberMapper.findByKakaoId(kakaoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "회원 정보를 찾을 수 없습니다."));
    }

    /** 카카오 로그인 시 회원을 조회하고, 없으면 생성한다(있으면 닉네임 갱신). */
    @Transactional
    public Member upsertByKakao(Long kakaoId, String nickname) {
        return memberMapper.findByKakaoId(kakaoId)
                .map(existing -> {
                    if (nickname != null && !nickname.equals(existing.getNickname())) {
                        memberMapper.updateNickname(Member.builder()
                                .kakaoId(kakaoId)
                                .nickname(nickname)
                                .build());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Member member = Member.builder()
                            .kakaoId(kakaoId)
                            .nickname(nickname)
                            .role("ROLE_USER")
                            .build();
                    memberMapper.insert(member);
                    return member;
                });
    }
}
