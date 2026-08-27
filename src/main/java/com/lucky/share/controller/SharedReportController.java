package com.lucky.share.controller;

import com.lucky.share.dto.SharedReportResponse;
import com.lucky.share.service.ReportShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공유 링크로 리포트 읽기. <b>로그인 없이</b> 열리는 유일한 리포트 경로다.
 * 토큰 하나가 곧 열쇠이므로, 여기서 내려보내는 값에 내부 식별자를 섞지 않는다.
 */
@RestController
@RequestMapping("/api/shared")
@RequiredArgsConstructor
public class SharedReportController {

    private final ReportShareService shareService;

    @GetMapping("/{token}")
    public SharedReportResponse read(@PathVariable String token) {
        return shareService.read(token);
    }
}
