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
