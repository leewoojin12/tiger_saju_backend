package com.lucky.payment.controller;

import com.lucky.member.domain.Member;
import com.lucky.member.service.MemberService;
import com.lucky.payment.dto.CompletePaymentRequest;
import com.lucky.payment.dto.PaymentResult;
import com.lucky.payment.dto.PreparePaymentRequest;
import com.lucky.payment.dto.PreparePaymentResponse;
import com.lucky.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MemberService memberService;

    /** 결제 준비: paymentId 발급. 로그인 필요 + CSRF. */
    @PostMapping("/prepare")
    public PreparePaymentResponse prepare(@Valid @RequestBody PreparePaymentRequest request,
                                          @AuthenticationPrincipal OAuth2User principal) {
        return paymentService.prepare(currentMember(principal).getId(), request);
    }

    /** 결제 완료 검증. 로그인 필요 + CSRF. */
    @PostMapping("/complete")
    public PaymentResult complete(@Valid @RequestBody CompletePaymentRequest request,
                                  @AuthenticationPrincipal OAuth2User principal) {
        return paymentService.complete(currentMember(principal).getId(), request.paymentId());
    }

    /**
     * 포트원 웹훅 수신. 인증/CSRF 제외(SecurityConfig). 서명 검증은 서비스에서.
     * 서명 검증을 위해 본문을 가공 없이 String 그대로 받는다.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature) {
        paymentService.handleWebhook(rawBody, webhookId, webhookTimestamp, webhookSignature);
        return ResponseEntity.ok().build();
    }

    private Member currentMember(OAuth2User principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Object id = principal.getAttribute("id");
        if (!(id instanceof Number kakaoId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return memberService.getByKakaoId(kakaoId.longValue());
    }
}
