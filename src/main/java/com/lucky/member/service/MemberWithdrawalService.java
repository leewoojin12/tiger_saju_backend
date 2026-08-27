package com.lucky.member.service;

import com.lucky.fortune.mapper.FortuneResultMapper;
import com.lucky.member.mapper.MemberMapper;
import com.lucky.review.mapper.ReportRatingMapper;
import com.lucky.review.mapper.ReviewMapper;
import com.lucky.saju.mapper.SajuHistoryMapper;
import com.lucky.share.mapper.ReportShareMapper;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 회원 탈퇴.
 *
 * <p>무엇을 지우고 무엇을 남기는지는 이미 공개해 둔 문서가 정한다. 임의로 정하지 않는다.
 * <ul>
 *   <li>이용약관 — "탈퇴 시 보관함의 콘텐츠와 미사용 혜택은 소멸하며 복구되지 않습니다."</li>
 *   <li>개인정보처리방침 — 카카오 회원번호·닉네임은 "탈퇴 시 지체 없이 파기",
 *       사주 입력 정보와 해설은 "결제일로부터 1년 또는 회원 탈퇴 시 중 먼저 도래하는 시점"에 파기</li>
 *   <li>같은 방침 — 계약·대금결제 기록은 전자상거래법에 따라 <b>5년 보관</b></li>
 * </ul>
 *
 * <p>그래서 payments 는 손대지 않는다. members 행도 지우지 않는다 — payments 가
 * member_id 로 걸려 있어서, 행을 지우면 5년간 보관해야 할 결제 기록의 주인이 사라진다.
 * 대신 그 행에서 사람을 알아볼 수 있는 값만 없앤다.
 *
 * <p>전부 한 트랜잭션이다. 중간에 실패하면 아무것도 지워지지 않은 상태로 되돌아간다 —
 * 절반만 지워진 회원이 남는 것이 가장 나쁘다.
 */
@Service
@RequiredArgsConstructor
public class MemberWithdrawalService {

    private final MemberMapper memberMapper;
    private final FortuneResultMapper resultMapper;
    private final SajuHistoryMapper historyMapper;
    private final ReportShareMapper shareMapper;
    private final ReportRatingMapper ratingMapper;
    private final ReviewMapper reviewMapper;

    @Transactional
    public void withdraw(Long memberId) {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. 공유 링크부터 끊는다. 남겨두면 탈퇴한 뒤에도 남의 리포트가 열린다.
        shareMapper.deleteByMemberId(memberId);

        // 2. 리포트 본문·입력값·이름 파기 + 보관함에서 제거. 행과 결제 연결은 남긴다.
        resultMapper.purgeByMemberId(memberId, now);

        // 3. 옛 보관함(saju_history)은 이름·생년월일이 컬럼에 그대로 있고 결제와 무관해 행째 삭제.
        historyMapper.deleteByMemberId(memberId);

        // 4. 별점은 회원과 끊을 수 없는 구조(member_id NOT NULL)라 삭제.
        ratingMapper.deleteByMemberId(memberId);

        // 5. 후기는 연결만 끊고 글은 남긴다(마스킹된 표기라 개인 식별 없음).
        reviewMapper.detachMember(memberId);

        // 6. 마지막에 회원 식별 정보 파기. 이미 탈퇴한 회원이면 0건 → 중복 요청으로 보고 조용히 통과.
        if (memberMapper.withdraw(memberId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 탈퇴 처리된 계정이에요.");
        }
    }
}
