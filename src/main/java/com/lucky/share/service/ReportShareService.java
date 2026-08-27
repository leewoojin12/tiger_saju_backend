package com.lucky.share.service;

import com.lucky.fortune.domain.FortuneResult;
import com.lucky.fortune.mapper.FortuneResultMapper;
import com.lucky.fortune.service.FortuneResultService;
import com.lucky.share.dto.ShareLinkResponse;
import com.lucky.share.dto.SharedReportResponse;
import com.lucky.share.dto.SharedRow;
import com.lucky.share.mapper.ReportShareMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 리포트 공유 링크.
 *
 * <p>보관함 주소(/content/{slug}/saved/{id})는 로그인 + 본인 소유여야 열리기 때문에,
 * 그 주소를 그대로 공유하면 받은 사람은 아무것도 못 본다. 대신 읽기 전용 토큰을 발급해
 * 로그인 없이 열리는 주소를 만든다.
 *
 * <p>설계상 지켜야 할 것 두 가지:
 * <ul>
 *   <li>토큰은 <b>해시로만</b> 저장한다. DB 를 읽을 수 있어도 살아있는 링크를 만들어낼 수 없다.</li>
 *   <li>열람 실패 이유를 구분해서 알려주지 않는다. 만료·해제·없는 토큰이 모두 같은 404 다.
 *       구분해 주면 남의 토큰을 넣어보며 "있긴 있다"는 사실을 알아낼 수 있다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReportShareService {

    /** 링크 수명. 공유는 대개 받자마자 열어보므로 하루면 충분하고, 오래 떠도는 것을 막는다. */
    private static final Duration TTL = Duration.ofDays(1);
    /** 토큰 길이(바이트). 32바이트면 추측으로 맞히는 건 사실상 불가능하다. */
    private static final int TOKEN_BYTES = 32;

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL64 = Base64.getUrlEncoder().withoutPadding();

    /** 링크가 죽었을 때 내보내는 단 하나의 메시지. 이유를 나누지 않는다. */
    private static final String GONE = "링크가 만료되었거나 잘못된 주소예요.";

    private final ReportShareMapper shareMapper;
    private final FortuneResultMapper resultMapper;

    /**
     * 공유 링크 발급. 본인이 결제해 받은, 완성된 리포트만.
     * 누를 때마다 새 토큰이 나오고, 이전 토큰도 각자의 만료 시각까지는 살아있다.
     */
    @Transactional
    public ShareLinkResponse create(Long resultId, Long memberId) {
        FortuneResult result = resultMapper.findByIdAndMemberId(resultId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "결과를 찾을 수 없습니다."));
        if (!"DONE".equals(result.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "완성된 리포트만 공유할 수 있어요.");
        }
        if (FortuneResultService.isRevoked(result.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "환불된 리포트는 공유할 수 없어요.");
        }
        if (FortuneResultService.isExpired(result.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "제공 기간이 지난 리포트는 공유할 수 없어요.");
        }

        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String token = URL64.encodeToString(raw);
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(TTL);
        shareMapper.insert(sha256Hex(token), resultId, memberId, expiresAt);
        return new ShareLinkResponse(token, expiresAt);
    }

    /**
     * 공유 링크로 읽기. 로그인 없이 호출된다.
     * 링크 수명뿐 아니라 리포트 쪽 사정(삭제·환불·제공기간 종료)도 함께 본다 —
     * 공유해 둔 뒤에 환불하면 링크도 같이 닫혀야 하기 때문이다.
     */
    @Transactional(readOnly = true)
    public SharedReportResponse read(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, GONE);
        }
        SharedRow row = shareMapper.findByTokenHash(sha256Hex(token))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, GONE));

        OffsetDateTime now = OffsetDateTime.now();
        boolean dead = row.revokedAt() != null
                || row.shareExpiresAt() == null || row.shareExpiresAt().isBefore(now)
                || row.deletedAt() != null
                || !"DONE".equals(row.status())
                || FortuneResultService.isRevoked(row.paymentStatus())
                || FortuneResultService.isExpired(row.resultExpiresAt());
        if (dead) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, GONE);
        }

        JsonNode result;
        try {
            result = JSON.readTree(row.resultJson());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "저장된 결과를 읽을 수 없습니다.");
        }
        return new SharedReportResponse(row.slug(), result, inputOf(row.inputJson()),
                row.shareExpiresAt());
    }

    /** input_json 은 {"input": GenerateRequest, "intro": "..."} 형태. 표시용이라 깨져 있으면 생략한다. */
    private static JsonNode inputOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(raw).get("input");
            return node != null && !node.isNull() ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다.", e);
        }
    }
}
