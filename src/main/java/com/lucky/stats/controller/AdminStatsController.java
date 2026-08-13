package com.lucky.stats.controller;

import com.lucky.stats.dto.StatsResponse;
import com.lucky.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 어드민 통계 대시보드 API. SecurityConfig 에서 /api/admin/** → hasRole("ADMIN") 로 잠겨 있음.
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping
    public StatsResponse dashboard() {
        return statsService.dashboard();
    }
}
