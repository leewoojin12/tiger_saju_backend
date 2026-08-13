package com.lucky.fortune.service;

import com.lucky.fortune.domain.Fortune;
import com.lucky.fortune.dto.FortunePublic;
import com.lucky.fortune.mapper.FortuneMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FortuneService {

    private final FortuneMapper fortuneMapper;

    public FortuneService(FortuneMapper fortuneMapper) {
        this.fortuneMapper = fortuneMapper;
    }

    /** 공개 카탈로그 목록 (active만, 프롬프트 제외). */
    public List<FortunePublic> listActive() {
        return fortuneMapper.findAllActive().stream()
                .map(FortunePublic::from)
                .toList();
    }

    /** 추천(인기) 탑5 — 완료된 풀 리포트 수가 많은 순. 메인 '추천 사주' 레일용. */
    public List<FortunePublic> listPopular() {
        return fortuneMapper.findPopularActive().stream()
                .map(FortunePublic::from)
                .toList();
    }

    /** 공개 상세 — 랜딩 페이지 가격/문구 (active만, 프롬프트 제외). */
    public FortunePublic getPublic(String slug) {
        return FortunePublic.from(getActiveEntity(slug));
    }

    /**
     * 내부용: active한 운세 엔티티(프롬프트 포함) 반환.
     * 결제 준비/생성 단계에서 가격·프롬프트를 신뢰 가능한 소스(DB)에서 가져올 때 사용.
     * 없거나 비활성이면 404 → 결제/생성 자체가 막힌다.
     */
    public Fortune getActiveEntity(String slug) {
        Fortune f = fortuneMapper.findBySlug(slug);
        if (f == null || !f.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 컨텐츠입니다.");
        }
        return f;
    }
}
