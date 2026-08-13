package com.lucky.payment.webhook;

import com.lucky.payment.config.PortOneProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * PortOne V2 웹훅 서명 검증 (Standard Webhooks 스펙).
 * 헤더: webhook-id, webhook-timestamp, webhook-signature
 * 서명 = base64( HMAC_SHA256( secret, "{id}.{timestamp}.{rawBody}" ) )
 * webhook-signature 헤더는 공백으로 구분된 "v1,{sig}" 목록.
 */
@Component
@RequiredArgsConstructor
public class PortOneWebhookVerifier {

    private static final long TOLERANCE_SECONDS = 300; // 타임스탬프 허용 오차(5분)

    private final PortOneProperties props;

    public void verify(String rawBody, String webhookId, String webhookTimestamp, String webhookSignature) {
        if (webhookId == null || webhookTimestamp == null || webhookSignature == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "웹훅 서명 헤더 누락");
        }

        // 1) 타임스탬프 재전송(replay) 방지
        final long ts;
        try {
            ts = Long.parseLong(webhookTimestamp.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "웹훅 타임스탬프 형식 오류");
        }
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - ts) > TOLERANCE_SECONDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "웹훅 타임스탬프 만료");
        }

        // 2) 시크릿 디코드 (whsec_ 접두사면 제거 후 base64 디코드)
        String secret = props.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "웹훅 시크릿 미설정");
        }
        byte[] key = secret.startsWith("whsec_")
                ? Base64.getDecoder().decode(secret.substring("whsec_".length()))
                : secret.getBytes(StandardCharsets.UTF_8);

        // 3) 기대 서명 계산
        String signedContent = webhookId + "." + webhookTimestamp + "." + rawBody;
        String expected = base64HmacSha256(key, signedContent);

        // 4) 헤더의 서명 목록과 상수 시간 비교
        boolean matched = false;
        for (String token : webhookSignature.split("\\s+")) {
            int comma = token.indexOf(',');
            String sig = comma >= 0 ? token.substring(comma + 1) : token;
            if (constantTimeEquals(sig, expected)) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "웹훅 서명 불일치");
        }
    }

    private static String base64HmacSha256(byte[] key, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "웹훅 서명 계산 실패", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
