# 결제 연동 (PortOne V2 — 토스 / 네이버 / 카카오페이)

## 0. 전체 흐름

```
[프론트] 1) POST /api/payments/prepare      → 서버가 paymentId 발급 + 기대금액 PENDING 저장
[프론트] 2) PortOne.requestPayment(...)      → 결제창(토스/네이버/카카오) 띄우고 결제
[프론트] 3) POST /api/payments/complete      → 서버가 포트원 재조회 + 금액검증 + 상태저장
[포트원] 4) POST /api/payments/webhook       → (안전장치) 결제상태 바뀔 때 포트원이 직접 호출
```
- 3번(complete)은 사용자 화면용 즉시 검증, 4번(webhook)은 사용자가 창을 닫거나 네트워크가 끊겨도
  결제 상태를 확실히 반영하는 **안전장치**. 둘 다 같은 검증 로직(포트원 재조회 + 금액 일치 확인)을 탄다.
- **상품/혜택 지급은 반드시 "검증된 결제(status=PAID, 금액 일치)" 기준**으로 처리할 것.

---

## 1. 키 셋팅

### 서버(비밀) — `.env`(로컬) / EC2 `/etc/lucky/lucky.env`(운영)
| 키 | 설명 |
|----|------|
| `PORTONE_API_SECRET` | V2 API Secret. 콘솔 > 결제연동 > **V2 API Secret**. 서버 전용, 절대 노출 금지 |
| `PORTONE_WEBHOOK_SECRET` | 웹훅 시크릿(`whsec_...`). 콘솔에서 웹훅 등록 시 발급 |
| `PORTONE_STORE_ID` | 상점 ID(`store-...`, 공개값, 참고용) |

EC2 적용:
```bash
sudo tee -a /etc/lucky/lucky.env >/dev/null <<'EOF'
PORTONE_API_SECRET=...
PORTONE_WEBHOOK_SECRET=whsec_...
PORTONE_STORE_ID=store-...
EOF
sudo systemctl restart lucky
```

### 프론트(공개값) — Vercel 환경변수 / `.env.local`
`storeId`, `channelKey`는 브라우저 SDK에서 쓰는 **공개값**이라 프론트 env에 둔다.
```
NEXT_PUBLIC_PORTONE_STORE_ID=store-xxxxxxxx
NEXT_PUBLIC_PORTONE_CHANNEL_KEY_TOSS=channel-key-xxxx     # 토스페이 채널
NEXT_PUBLIC_PORTONE_CHANNEL_KEY_NAVER=channel-key-yyyy    # 네이버페이 채널
NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAO=channel-key-zzzz    # 카카오페이 채널
```

### 콘솔 셋팅 순서 (admin.portone.io)
1. 결제연동 > **V2 API Secret** 발급 → `PORTONE_API_SECRET`
2. 결제연동 > 채널: **토스페이 / 네이버페이 / 카카오페이** 각각 채널 추가(계약/테스트) → 각 `channelKey` 복사
3. 상점 ID(`store-...`) 복사 → `PORTONE_STORE_ID` / 프론트 env
4. 결제연동 > **웹훅** 추가:
   - URL: `https://api.trendsaju.com/api/payments/webhook`
   - 버전: **V2**, 포맷: JSON
   - 발급된 시크릿(`whsec_...`) → `PORTONE_WEBHOOK_SECRET`

---

## 2. 백엔드 엔드포인트

### POST `/api/payments/prepare` (로그인 필요 + CSRF)
```jsonc
// req
{ "orderName": "사주 프리미엄 풀이", "amount": 9900 }
// res
{ "paymentId": "saju_xx…", "orderName": "사주 프리미엄 풀이",
  "amount": 9900, "currency": "KRW", "storeId": "store-…" }
```
> ⚠️ 지금은 `amount`를 프론트가 보냄. 운영에선 상품/플랜 코드로 **서버가 가격을 결정**하도록 바꾸는 걸 권장.

### POST `/api/payments/complete` (로그인 필요 + CSRF)
```jsonc
// req
{ "paymentId": "saju_xx…" }
// res
{ "paymentId": "saju_xx…", "status": "PAID", "amount": 9900, "paid": true }
```
- 400: 금액 불일치(위변조 의심) / 404: 결제건 없음 / 403: 타인 결제건

### POST `/api/payments/webhook` (인증·CSRF 제외, 포트원 전용)
- 포트원이 호출. `webhook-id` / `webhook-timestamp` / `webhook-signature` 헤더로 서명 검증(Standard Webhooks).
- 프론트가 호출할 일 없음.

---

## 3. 프론트 결제 호출 (브라우저 SDK)

설치:
```bash
npm install @portone/browser-sdk
```

```ts
import PortOne from '@portone/browser-sdk/v2';
import { apiFetch } from '@/lib/api'; // 앞서 만든 공통 래퍼(credentials + CSRF)

const STORE_ID = process.env.NEXT_PUBLIC_PORTONE_STORE_ID!;

type Provider = 'TOSS' | 'NAVER' | 'KAKAO';

const CHANNEL_KEY: Record<Provider, string> = {
  TOSS:  process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_TOSS!,
  NAVER: process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_NAVER!,
  KAKAO: process.env.NEXT_PUBLIC_PORTONE_CHANNEL_KEY_KAKAO!,
};
const EASY_PAY_PROVIDER: Record<Provider, string> = {
  TOSS:  'EASY_PAY_PROVIDER_TOSSPAY',
  NAVER: 'EASY_PAY_PROVIDER_NAVERPAY',
  KAKAO: 'EASY_PAY_PROVIDER_KAKAOPAY',
};

export async function pay(provider: Provider) {
  // 1) 서버에 결제 준비 요청 → paymentId/금액 확보
  const prepRes = await apiFetch('/api/payments/prepare', {
    method: 'POST',
    body: JSON.stringify({ orderName: '사주 프리미엄 풀이', amount: 9900 }),
  });
  if (!prepRes.ok) throw new Error('prepare 실패');
  const { paymentId, orderName, amount } = await prepRes.json();

  // 2) 결제창 호출
  const result = await PortOne.requestPayment({
    storeId: STORE_ID,
    channelKey: CHANNEL_KEY[provider],
    paymentId,
    orderName,
    totalAmount: amount,
    currency: 'CURRENCY_KRW',
    payMethod: 'EASY_PAY',
    easyPay: { easyPayProvider: EASY_PAY_PROVIDER[provider] },
  });

  // 사용자가 취소하거나 실패하면 code 가 채워져서 옴
  if (result?.code != null) {
    alert(`결제 실패: ${result.message}`);
    return;
  }

  // 3) 서버 검증
  const compRes = await apiFetch('/api/payments/complete', {
    method: 'POST',
    body: JSON.stringify({ paymentId }),
  });
  const data = await compRes.json();
  if (data.paid) {
    // 결제 완료 처리 (보관함/프리미엄 해제 등)
  } else {
    alert(`결제 상태: ${data.status}`);
  }
}
```

> - 토스/네이버/카카오는 위처럼 `channelKey` + `easyPayProvider`만 바꾸면 됨.
> - 네이버페이 등 일부 PG는 추가 파라미터(상품 정보 등)를 요구할 수 있음 → 포트원 PG별 문서 참고.
> - `requestPayment` 는 결제창 리다이렉트가 일어날 수 있으므로, 모바일/리다이렉트 환경에선
>   `redirectUrl` 옵션 + 완료 페이지에서 `complete` 호출 패턴도 고려.

---

## 4. 보안 체크리스트
- [ ] `PORTONE_API_SECRET` / `PORTONE_WEBHOOK_SECRET` 은 **서버에만**. 프론트/깃 금지.
- [ ] 금액은 서버 저장값(prepare) 기준으로 검증 — 클라이언트 금액 신뢰 금지.
- [ ] 웹훅 서명 검증 필수(구현됨). URL은 공개되므로 서명 없는 요청은 거부.
- [ ] 혜택 지급은 `status=PAID` + 금액 일치일 때만.
