package com.lucky.payment.controller;

import com.lucky.fortune.dto.EnqueueResponse;
import com.lucky.fortune.dto.GenerateRequest;
import com.lucky.fortune.service.FortuneGenerationService;
import com.lucky.member.domain.Member;
import com.lucky.member.service.MemberService;
import com.lucky.payment.domain.Payment;
import com.lucky.payment.dto.CompletePaymentRequest;
import com.lucky.payment.dto.PaymentResult;
import com.lucky.payment.dto.PreparePaymentRequest;
import com.lucky.payment.dto.PreparePaymentResponse;
import com.lucky.payment.dto.UnfulfilledPayment;
import com.lucky.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final PaymentService paymentService;
    private final MemberService memberService;
    private final FortuneGenerationService generationService;

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
     * 결제는 됐는데 리포트가 없는 내 결제건 목록.
     * (결제 직후 창을 닫는 등으로 생성 요청이 유실된 경우 보관함에서 안내)
     */
    @GetMapping("/unfulfilled")
    public List<UnfulfilledPayment> unfulfilled(@AuthenticationPrincipal OAuth2User principal) {
        return paymentService.listUnfulfilled(currentMember(principal).getId());
    }

    /** 미수령 결제건으로 리포트 생성 재개(저장된 입력 사용). 이미 있으면 그 결과를 그대로 반환. */
    @PostMapping("/{paymentId}/fulfill")
    public EnqueueResponse fulfill(@PathVariable String paymentId,
                                   @AuthenticationPrincipal OAuth2User principal) {
        Long memberId = currentMember(principal).getId();
        Payment p = paymentService.requireResumable(memberId, paymentId);
        GenerateRequest input;
        try {
            input = JSON.readValue(p.getInputJson(), GenerateRequest.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "저장된 입력을 읽을 수 없습니다.", e);
        }
        return generationService.enqueueFull(p.getProductCode(), paymentId, input, null, memberId);
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
