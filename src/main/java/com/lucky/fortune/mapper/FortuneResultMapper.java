package com.lucky.fortune.mapper;

import com.lucky.fortune.domain.FortuneResult;
import com.lucky.fortune.dto.MyResultSummary;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FortuneResultMapper {

    /** GENERATING 행 INSERT. payment_id 중복 시 ON CONFLICT DO NOTHING → 0 반환(이미 있음). */
    int insertGenerating(FortuneResult result);

    /** 워커가 id로 행 조회(소유권 무관, 내부용). */
    Optional<FortuneResult> findById(@Param("id") Long id);

    /** 생성 성공 → result_json 채우고 status='DONE', error 초기화. */
    int markDone(@Param("id") Long id, @Param("resultJson") String resultJson);

    /** 생성 실패 → status='FAILED', error 기록. */
    int markFailed(@Param("id") Long id, @Param("error") String error);

    /** 재시도 → status='GENERATING' 으로 되돌리고 error 초기화. */
    int markGenerating(@Param("id") Long id);

    /** 결제건 기준 조회 (멱등 생성 + 재조회/PDF용). */
    Optional<FortuneResult> findByPaymentId(@Param("paymentId") String paymentId);

    /** 보관함 목록 (본인 것, 최신순). 본문 제외 요약만. */
    List<MyResultSummary> findSummariesByMemberId(@Param("memberId") Long memberId);

    /**
     * 보관함 상세 (본인 것 · 삭제되지 않은 것만). 소유권 검증 겸용.
     * payments 를 조인해 {@code paymentStatus} 를 함께 채운다(환불 리포트 열람 차단용).
     */
    Optional<FortuneResult> findByIdAndMemberId(@Param("id") Long id,
                                                @Param("memberId") Long memberId);

    /** 보관함에서 리포트 지우기(소프트 삭제). 본인 것만. 반영된 행 수(0이면 없음/이미 삭제). */
    int softDeleteByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    /** GENERATING 인 채 방치된(=워커가 죽은) 행. before 이전에 시작된 것만. */
    List<FortuneResult> findStuckGenerating(@Param("before") OffsetDateTime before);
}
