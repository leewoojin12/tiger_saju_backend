# 코딩 스타일 규칙

## 네이밍
- 클래스/인터페이스: PascalCase (인터페이스에 `I` 접두사 금지).
- 메서드/변수: camelCase. 상수: `UPPER_SNAKE_CASE`.
- 컨트롤러 `XxxController`, 서비스 `XxxService`, Mapper `XxxMapper`, DTO `XxxRequest`/`XxxResponse`.
- 패키지명은 전부 소문자.

## 레이어 규칙
- 의존 방향은 controller → service → mapper 한 방향만. 역방향 금지.
- controller는 Mapper를 직접 호출하지 않는다.
- DTO ↔ 도메인 변환은 service(또는 전용 변환기)에서. 도메인 객체를 그대로 뷰/응답에 노출하지 않는다.
- 외부 연동(S3 / OpenAI)은 인터페이스로 추상화해 service가 그 인터페이스에 의존하게 한다.

## MyBatis Mapper 인터페이스 ↔ XML 매칭
- XML `<mapper namespace="...">` 는 Mapper 인터페이스의 FQN과 정확히 일치.
- `<select|insert|update|delete>` 의 `id` 는 인터페이스 메서드명과 일치.
- 파라미터·반환 타입이 인터페이스 시그니처와 일치.
- XML 위치: `src/main/resources/mapper/` 아래, 인터페이스 패키지 구조를 따른다.

## 기타
- Lombok 사용 가능(`@Getter`, `@RequiredArgsConstructor` 등). 엔티티에 `@Data`는 신중히.
- 생성자 주입 사용(필드 `@Autowired` 지양).
- 매직 넘버·하드코딩 문자열은 상수/설정으로 분리.
