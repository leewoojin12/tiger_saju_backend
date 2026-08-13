package com.lucky.fortune.controller;

import com.lucky.fortune.dto.FortuneAdminView;
import com.lucky.fortune.dto.FortuneUpsertRequest;
import com.lucky.fortune.service.FortuneAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 운세 관리 API. SecurityConfig에서 /api/admin/** → hasRole("ADMIN") 로 잠겨 있음.
 * (비개발자 운영자용 — 프롬프트/가격/노출 편집. POST·PUT 은 CSRF 필요.)
 */
@RestController
@RequestMapping("/api/admin/fortunes")
public class AdminFortuneController {

    private final FortuneAdminService adminService;

    public AdminFortuneController(FortuneAdminService adminService) {
        this.adminService = adminService;
    }

    /** 전체 목록 (비활성 포함, 프롬프트 포함). */
    @GetMapping
    public List<FortuneAdminView> list() {
        return adminService.list();
    }

    /** 단건 (편집 폼 로드용). */
    @GetMapping("/{slug}")
    public FortuneAdminView get(@PathVariable String slug) {
        return adminService.get(slug);
    }

    /** 신규 생성. */
    @PostMapping
    public FortuneAdminView create(@Valid @RequestBody FortuneUpsertRequest request) {
        return adminService.create(request);
    }

    /** 수정 (slug 는 경로로 식별, 변경 불가). */
    @PutMapping("/{slug}")
    public FortuneAdminView update(@PathVariable String slug,
                                   @Valid @RequestBody FortuneUpsertRequest request) {
        return adminService.update(slug, request);
    }

    /** 삭제. 결제·결과가 있는 운세는 백엔드에서 409 로 거부한다(이력 보존). */
    @DeleteMapping("/{slug}")
    public void delete(@PathVariable String slug) {
        adminService.delete(slug);
    }
}
