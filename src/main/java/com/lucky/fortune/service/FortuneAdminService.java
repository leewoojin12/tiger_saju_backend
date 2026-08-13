package com.lucky.fortune.service;

import com.lucky.fortune.domain.Fortune;
import com.lucky.fortune.dto.FortuneAdminView;
import com.lucky.fortune.dto.FortuneUpsertRequest;
import com.lucky.fortune.mapper.FortuneMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 관리자(ROLE_ADMIN) 전용 운세 CRUD. 프롬프트/가격/노출 관리. */
@Service
@RequiredArgsConstructor
public class FortuneAdminService {

    private final FortuneMapper fortuneMapper;

    @Transactional(readOnly = true)
    public List<FortuneAdminView> list() {
        return fortuneMapper.findAllForAdmin().stream().map(FortuneAdminView::from).toList();
    }

    @Transactional(readOnly = true)
    public FortuneAdminView get(String slug) {
        return FortuneAdminView.from(require(slug));
    }

    @Transactional
    public FortuneAdminView create(FortuneUpsertRequest req) {
        if (fortuneMapper.findBySlug(req.slug()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 slug 입니다: " + req.slug());
        }
        Fortune f = toEntity(req, req.slug());
        fortuneMapper.insert(f);
        return FortuneAdminView.from(f);
    }

    @Transactional
    public FortuneAdminView update(String slug, FortuneUpsertRequest req) {
        require(slug);                       // 없으면 404
        fortuneMapper.updateBySlug(toEntity(req, slug));  // slug 는 경로 값으로 고정
        return get(slug);
    }

    /** 삭제. 결제/생성 이력이 남아 있으면(FK) 이력 보존을 위해 409 로 거부. */
    @Transactional
    public void delete(String slug) {
        Fortune f = require(slug);
        try {
            fortuneMapper.deleteBySlug(f.getSlug());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "결제·리포트 이력이 있는 컨텐츠는 삭제할 수 없어요. 대신 '노출(active)'을 꺼 주세요."
            );
        }
    }

    private Fortune require(String slug) {
        Fortune f = fortuneMapper.findBySlug(slug);
        if (f == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "컨텐츠를 찾을 수 없습니다: " + slug);
        }
        return f;
    }

    private Fortune toEntity(FortuneUpsertRequest req, String slug) {
        Fortune f = new Fortune();
        f.setSlug(slug);
        f.setTitle(req.title());
        f.setDescription(req.description());
        f.setDurationText(req.durationText());
        f.setPrice(req.price());
        f.setActive(req.active());
        // 카테고리 미지정 시 '기타' 기본값(컬럼 DEFAULT 와 일치).
        f.setCategory(req.category() == null || req.category().isBlank() ? "기타" : req.category());
        f.setTeaserPrompt(req.teaserPrompt());
        f.setFullPrompt(req.fullPrompt());
        f.setUiConfig(req.uiConfig());
        return f;
    }
}
