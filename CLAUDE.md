# lucky 프로젝트 가이드 (CLAUDE.md)

## 스택
- 언어/프레임워크: Java 17, Spring Boot 4.1 (빌드: Gradle, Groovy DSL)
- 뷰: Thymeleaf + HTML (서버사이드 렌더링, SSR)
- DB: MariaDB + MyBatis (XML Mapper)
- 파일 저장: AWS S3
- 외부 API: OpenAI ChatGPT API (LLM)
- 기본 패키지: `com.lucky`

## 패키지 구조 컨벤션
기능(도메인) 단위로 묶고, 그 안에서 레이어를 나눈다.

```
com.lucky
├── <feature>/
│   ├── controller/   # 요청/응답, 화면 또는 REST 엔드포인트
│   ├── service/      # 비즈니스 로직, 트랜잭션 경계
│   ├── mapper/       # MyBatis Mapper 인터페이스
│   ├── dto/          # 요청/응답 DTO
│   └── domain/       # 도메인 객체
├── common/           # 공통 응답·예외
└── config/           # 스프링 설정 (S3, OpenAI, MyBatis 등)
```

- MyBatis Mapper XML: `src/main/resources/mapper/<Feature>Mapper.xml`

## 레이어 분리 규칙
- **controller**: 입력 검증 + 요청/응답 변환만. 비즈니스 로직 금지. Mapper 직접 호출 금지(반드시 service 경유).
- **service**: 비즈니스 로직과 트랜잭션 경계(`@Transactional`). 외부 API(OpenAI, S3) 호출 조율.
- **mapper**: SQL 전용. 비즈니스 로직 없음.

## 공통 응답 / 예외 처리
- REST 응답은 공통 래퍼(`ApiResponse<T>`: `code`, `message`, `data`)로 통일.
- 예외는 전역 핸들러(`@RestControllerAdvice`)에서 처리. 도메인 예외는 `BusinessException` 등으로 표준화.
- 스택트레이스·내부 메시지·시크릿을 사용자 응답에 노출하지 않는다.
- 화면(Thymeleaf) 흐름과 REST 흐름의 에러 처리를 구분한다.

## DB 설정
- 로컬은 docker MariaDB (`saju` DB, 3306). 접속 정보는 `application.yaml`.
- 시크릿(비밀번호 등)은 환경변수로 주입한다(`${DB_PASSWORD:...}`). 코드/설정에 실제 시크릿 하드코딩 금지.

## 세부 규칙
아래 규칙 문서를 항상 따른다.

@.claude/rules/coding-style.md
@.claude/rules/security.md
@.claude/rules/llm-integration.md
@.claude/rules/testing.md
