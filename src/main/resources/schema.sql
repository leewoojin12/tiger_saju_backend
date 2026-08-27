-- members 테이블 (카카오 로그인 사용자) — PostgreSQL / Supabase
-- 실행 방법(택1):
--   1) Supabase 대시보드 → SQL Editor 에 아래 DDL 붙여넣고 1회 실행 (권장)
--   2) application.yaml 에 spring.sql.init.mode: always 추가 → 부팅 시 자동 실행 (IF NOT EXISTS 라 멱등)
CREATE TABLE IF NOT EXISTS members (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kakao_id    BIGINT       NOT NULL,
    nickname    VARCHAR(100),
    role        VARCHAR(30)  NOT NULL DEFAULT 'ROLE_USER',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_members_kakao_id UNIQUE (kakao_id)
);

-- saju_history : 사주 풀이 결과 보관함
--  - member_id : 소유자(로그인 회원). 보관함은 본인 것만 조회.
--  - result_json : SajuResponse 전체 JSON (상세 조회 시 그대로 역직렬화)
--  - ilju_name / summary : 목록 미리보기용 비정규화 컬럼(목록에서 JSON 파싱 회피)
CREATE TABLE IF NOT EXISTS saju_history (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id     BIGINT       NOT NULL REFERENCES members(id),
    username      VARCHAR(100),
    name          VARCHAR(100) NOT NULL,
    gender        VARCHAR(10),
    calendar      VARCHAR(10),
    birth_date    DATE         NOT NULL,
    birth_time    VARCHAR(40),
    time_unknown  BOOLEAN      NOT NULL DEFAULT false,
    ilju_name     VARCHAR(50),
    summary       TEXT,
    result_json   TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_saju_history_member ON saju_history (member_id, created_at DESC);

-- payments : 포트원(PortOne) V2 결제 내역
--  - payment_id : 포트원 결제건 ID(서버가 prepare 시 생성, 프론트 requestPayment 에 전달). 멱등/조회 키
--  - amount     : 서버가 기대하는 결제 금액(원). 검증 시 포트원 amount.total 과 일치해야 함(위변조 방지)
--  - status     : PENDING(준비) → PAID/FAILED/CANCELLED/VIRTUAL_ACCOUNT_ISSUED ...(포트원 상태 반영)
--  - raw_json   : 포트원 결제객체 원본(감사/디버깅용)
CREATE TABLE IF NOT EXISTS payments (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    payment_id  VARCHAR(100) NOT NULL UNIQUE,
    member_id   BIGINT       REFERENCES members(id),
    order_name  VARCHAR(200),
    amount      BIGINT       NOT NULL,
    currency    VARCHAR(10)  NOT NULL DEFAULT 'KRW',
    pay_method  VARCHAR(40),
    status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    pg_tx_id    VARCHAR(100),
    raw_json    TEXT,
    paid_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payments_member ON payments (member_id, created_at DESC);

-- 결제건이 어떤 컨텐츠(fortunes.slug)에 대한 것인지. prepare 시 서버가 채움 → "이 회원이 이 운세 PAID인가" 판단 키.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS product_code VARCHAR(60);
CREATE INDEX IF NOT EXISTS idx_payments_member_product ON payments (member_id, product_code, status);

-- fortunes : 사주 컨텐츠(운세) 카탈로그
--  - slug          : 폴더명/상품코드 (프론트 페이지와 매칭, 결제·생성의 키)
--  - price         : 가격(원)
--  - active        : 노출 on/off (off여도 페이지는 접근 가능 → 결제/생성에서 서버가 막음)
--  - teaser_prompt : 무료 맛보기용 짧은 프롬프트
--  - full_prompt   : 유료 풀 리포트용 프롬프트  (둘 다 "이 JSON 형식으로 답해"까지 포함시킬 것)
--  ⚠️ 프롬프트(teaser/full)는 서버 전용 — 공개 API로 절대 내보내지 말 것
CREATE TABLE IF NOT EXISTS fortunes (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug          VARCHAR(60)  NOT NULL UNIQUE,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    duration_text VARCHAR(50),
    price         BIGINT       NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT true,
    teaser_prompt TEXT,
    full_prompt   TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 카테고리: 메인 탭 필터용(연애/재회/결혼/가정/기타). '무료' 탭은 price=0 으로 별도 판단(컬럼 아님).
ALTER TABLE fortunes ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT '기타';

-- 공용 입력폼/미리보기 구성(호랑이풍수): 질문·고민불릿·챕터목록 JSON. 공개 가능(프롬프트 아님).
ALTER TABLE fortunes ADD COLUMN IF NOT EXISTS ui_config TEXT;

-- fortune_results : 결제 완료(PAID) 후 생성된 '풀 리포트' 결과. 결제 1건당 1행.
--  - payment_id  : 어떤 결제건의 결과인지 (UNIQUE → 결제당 1회 생성, 재호출 멱등)
--  - slug        : 어떤 컨텐츠인지 (fortunes.slug)
--  - result_json : 풀 리포트 AI JSON. PDF·재조회 시 AI 재호출 없이 그대로 사용.
CREATE TABLE IF NOT EXISTS fortune_results (
    id          BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id   BIGINT       NOT NULL REFERENCES members(id),
    slug        VARCHAR(60)  NOT NULL,
    payment_id  VARCHAR(100) NOT NULL UNIQUE REFERENCES payments(payment_id),
    name        VARCHAR(100),
    result_json TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_fortune_results_member ON fortune_results (member_id, created_at DESC);

-- 비동기(폴링) 생성 지원:
--  - status      : GENERATING(생성 중) → DONE(완료) / FAILED(실패). 기존 행은 DEFAULT 'DONE' 으로 그대로 열림.
--  - error       : FAILED 시 사용자 안내 메시지(없으면 NULL)
--  - input_json  : 재시도(/retry)용 생성 입력 보관 {"input":GenerateRequest,"intro":...}. 완료 후에도 둬도 무해.
--  - result_json : 생성 시작 시점엔 아직 없음 → NOT NULL 제거(완료 시 채움)
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS status     TEXT NOT NULL DEFAULT 'DONE';
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS error      TEXT;
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS input_json TEXT;
ALTER TABLE fortune_results ALTER COLUMN result_json DROP NOT NULL;

-- 결제 준비 시 사용자의 입력(subjects/answers) 스냅샷.
-- 결제 후 브라우저를 닫아도 서버가 리포트를 만들어 줄 수 있게 한다(미수령 결제 복구).
ALTER TABLE payments ADD COLUMN IF NOT EXISTS input_json TEXT;

-- 생성 실패 정책:
--  - attempts  : 생성 시도 횟수(최초 1회 + 재시도). MAX_ATTEMPTS 도달 시 자동 환불 + 재시도 차단.
--  - failed_at : 마지막 실패 시각(운영 추적용)
--  - started_at: 생성이 시작된(=GENERATING 이 된) 시각. 배포/장애로 워커가 죽어 GENERATING 인 채
--                방치된 좀비 행을 스케줄러가 골라내는 기준. created_at 을 쓰면 재시도 행이 오판된다.
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS attempts   INT NOT NULL DEFAULT 0;
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS failed_at  TIMESTAMPTZ;
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;
UPDATE fortune_results SET started_at = created_at WHERE started_at IS NULL;

-- 보관함에서 사용자가 리포트를 지웠을 때(소프트 삭제). 결제 이력(payments)은 그대로 보존한다.
-- 하드 삭제하면 (1)미수령 결제 배너가 되살아나고 (2)재결제 없이 재생성이 가능해지므로 반드시 소프트 삭제.
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_fortune_results_member_alive
    ON fortune_results (member_id, created_at DESC) WHERE deleted_at IS NULL;

-- 운세 정렬 순서: 낮은 숫자가 앞. NULL 이면 등록순(id)으로 뒤에 붙는다.
ALTER TABLE fortunes ADD COLUMN IF NOT EXISTS sort_order INT;

-- 보관함 제목 = '산 시점의 상품명' 스냅샷.
-- 원래는 목록 조회에서 slug 으로 fortunes 를 조인해 제목을 가져왔는데, admin 에서 상품 slug 을
-- 바꾸면 예전 리포트의 조인이 끊겨 제목이 사라졌다(보관함에 "사주 리포트"로만 표시).
-- 상품명을 바꿔도 이미 팔린 리포트는 그때 이름 그대로여야 하므로 생성 시 값을 박아 둔다.
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS title VARCHAR(100);

-- slug 이 바뀌기 전에 만들어진 옛 리포트들의 slug 정정.
-- (money → wealth[재물운], health1 → health[건강운]. 바뀐 slug 이 실제로 있을 때만 옮긴다.)
-- 제목뿐 아니라 보관함 썸네일 조회도 slug 으로 하므로 같이 살아난다.
UPDATE fortune_results r SET slug = 'wealth'
 WHERE r.slug = 'money'   AND EXISTS (SELECT 1 FROM fortunes f WHERE f.slug = 'wealth');
UPDATE fortune_results r SET slug = 'health'
 WHERE r.slug = 'health1' AND EXISTS (SELECT 1 FROM fortunes f WHERE f.slug = 'health');

-- 기존 행 채우기: 지금 카탈로그에서 찾아지는 것만. 못 찾으면 NULL 로 두고 화면에서 기본 문구로 뺀다.
UPDATE fortune_results r SET title = f.title
  FROM fortunes f
 WHERE r.title IS NULL AND f.slug = r.slug;

-- 콘텐츠 제공 기간(약관 제9조 / 환불정책 제2조): 결제일로부터 1년.
--  - expires_at 이 지나면 보관함에서 열람이 종료되고, 배치가 본문·입력값을 파기한다.
--  - 행 자체는 남긴다. 지우면 (1)미수령 결제 배너가 되살아나고 (2)payment_id 멱등이 풀려
--    결제 없이 재생성이 가능해진다(soft delete 와 같은 이유).
--  - 기간을 나중에 바꾸더라도 이미 판 건의 만료일이 흔들리지 않도록 계산값이 아닌 컬럼으로 박아 둔다.
ALTER TABLE fortune_results ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
UPDATE fortune_results SET expires_at = created_at + INTERVAL '1 year' WHERE expires_at IS NULL;
-- 파기 배치가 훑는 경로: 아직 본문이 남아 있는 만료 건.
CREATE INDEX IF NOT EXISTS idx_fortune_results_expiring
    ON fortune_results (expires_at) WHERE result_json IS NOT NULL;

-- reviews : 이용 후기. 홈 상단 후기 띠와 상품 상세에서 읽는다.
--  - result_id : 어떤 리포트에 대한 후기인지. 결제하고 받은 리포트 1건당 후기 1개(부분 UNIQUE).
--                이관/운영자 등록 건은 연결할 리포트가 없으므로 NULL 허용.
--  - author    : 화면에 찍히는 이름. 실명을 그대로 두지 않고 마스킹해서(김**) 넣는다.
--  - product   : 후기를 쓴 시점의 상품명 스냅샷. slug 이 바뀌어도 후기 표시는 안 흔들린다.
--  - status    : PENDING(검토 대기) / PUBLIC(공개) / HIDDEN(숨김). 공개 API 는 PUBLIC 만 내보낸다.
--                기본값을 PENDING 으로 둬서, 확인하지 않은 글이 저절로 노출되는 일이 없게 한다.
--  - written_at: 화면에 표시하는 작성일. 이관 건은 원본 날짜를 그대로 넣기 위해 created_at 과 분리.
CREATE TABLE IF NOT EXISTS reviews (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id  BIGINT       REFERENCES members(id),
    result_id  BIGINT       REFERENCES fortune_results(id),
    slug       VARCHAR(60),
    product    VARCHAR(100),
    author     VARCHAR(50)  NOT NULL,
    rating     SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body       TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    written_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- 리포트 1건당 후기 1개(연결 없는 이관 건은 제외).
CREATE UNIQUE INDEX IF NOT EXISTS uk_reviews_result
    ON reviews (result_id) WHERE result_id IS NOT NULL;
-- 홈 조회 경로: 공개된 것만 최신순.
CREATE INDEX IF NOT EXISTS idx_reviews_public
    ON reviews (written_at DESC) WHERE status = 'PUBLIC';

-- report_ratings : 리포트 하단의 별점(★1~5). 후기(reviews)와는 다른 행위라 테이블을 나눈다.
--  - reviews 는 본문(body NOT NULL)과 검토 상태(PENDING/PUBLIC/HIDDEN)를 가진 '글'이고,
--    여기는 글 없이 점수만 남기는 한 번의 클릭이다. 검토 대상도 아니다.
--  - result_id UNIQUE : 리포트 1건당 한 번만. 다시 누르거나 새로고침해도 덮어쓰지 않는다.
--  - slug 스냅샷 : 상품별 평균을 뽑을 때 fortune_results 조인 없이 집계할 수 있게.
CREATE TABLE IF NOT EXISTS report_ratings (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    result_id  BIGINT       NOT NULL REFERENCES fortune_results(id),
    member_id  BIGINT       NOT NULL REFERENCES members(id),
    slug       VARCHAR(60),
    rating     SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_report_ratings_result UNIQUE (result_id)
);
-- 상품별 평균·분포 집계 경로.
CREATE INDEX IF NOT EXISTS idx_report_ratings_slug ON report_ratings (slug);

-- report_shares : 리포트 공유 링크. 링크를 받은 사람은 로그인 없이 읽기만 할 수 있다.
--  - token_hash : 토큰 원본이 아니라 SHA-256 해시를 저장한다. DB 를 들여다봐도
--                 살아있는 공유 링크를 만들어낼 수 없게 하기 위함(토큰은 발급 순간에만 존재).
--  - expires_at : 발급 시점 + 1일. 지나면 링크는 '없는 주소'와 똑같이 취급된다.
--  - revoked_at : 링크가 새어 나갔을 때 끄는 비상구. 지금은 화면이 없고 DB 로만 조작한다.
--                 (UPDATE report_shares SET revoked_at = now() WHERE result_id = ...)
CREATE TABLE IF NOT EXISTS report_shares (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_hash CHAR(64)     NOT NULL,
    result_id  BIGINT       NOT NULL REFERENCES fortune_results(id),
    member_id  BIGINT       NOT NULL REFERENCES members(id),
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_report_shares_token UNIQUE (token_hash)
);
-- 한 리포트의 링크를 한꺼번에 끊을 때 쓰는 경로.
CREATE INDEX IF NOT EXISTS idx_report_shares_result ON report_shares (result_id);

-- 회원 탈퇴 표시. 행 자체는 남긴다 — payments 가 member_id 로 걸려 있고,
-- 결제·분쟁 기록은 전자상거래법상 5년을 보관해야 하기 때문(개인정보처리방침 3장).
-- 탈퇴 시 kakao_id 는 식별 불가능한 값으로 덮고 nickname 은 비운다(같은 장, '탈퇴 시 지체 없이 파기').
ALTER TABLE members ADD COLUMN IF NOT EXISTS withdrawn_at TIMESTAMPTZ;
