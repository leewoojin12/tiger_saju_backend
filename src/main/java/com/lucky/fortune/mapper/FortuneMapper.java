package com.lucky.fortune.mapper;

import com.lucky.fortune.domain.Fortune;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FortuneMapper {

    /** 노출(active=true) 운세 목록. 프롬프트 컬럼은 select하지 않음. */
    List<Fortune> findAllActive();

    /** 인기 탑5: 완료된 풀 리포트(fortune_results, DONE) 수가 많은 순. active만. */
    List<Fortune> findPopularActive();

    /** slug로 1건 조회 (프롬프트 포함, active 무관). 없으면 null. */
    Fortune findBySlug(String slug);

    // --- 관리자(admin) 전용 ---

    /** 전체 목록(비활성 포함, 프롬프트 포함). 관리 화면용. */
    List<Fortune> findAllForAdmin();

    /** 신규 컨텐츠 생성. */
    void insert(Fortune fortune);

    /** 기존 컨텐츠 수정 (slug 기준). slug 자체는 변경하지 않음. */
    void updateBySlug(Fortune fortune);

    /** 삭제. 결제/결과 FK 가 남아 있으면 DataIntegrityViolationException. */
    int deleteBySlug(String slug);
}
