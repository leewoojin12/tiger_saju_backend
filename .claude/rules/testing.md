# 테스트 규칙

- JUnit 5(Jupiter) 기반. 단언은 AssertJ 권장.
- 테스트 클래스 `XxxTest`, 메서드명은 의도를 드러내게(`should_...`, `given_when_then`).

## 레이어별
- **service**: 비즈니스 로직 단위 테스트. 의존(mapper, 외부 API)은 Mockito로 mock.
- **mapper**: 실제 SQL 검증이 목적. 슬라이스 테스트(`@MybatisTest` 등) + 테스트용 DB(H2 또는 Testcontainers MariaDB)로 검증. `${}` 회귀 방지 케이스 포함.
- **controller**: MockMvc(또는 RestTestClient)로 요청/응답·검증·예외 매핑을 테스트.

## 외부 연동
- **OpenAI / S3 호출은 실제로 때리지 않는다.** 반드시 mock/stub. 실제 API 키가 테스트에서 쓰이면 안 된다.
- 재시도·타임아웃·429 처리 로직은 mock으로 시나리오를 구성해 검증한다.

## 기타
- 테스트는 외부 상태에 의존하지 않고 반복 실행 가능해야 한다.
- 새 기능/버그 수정 시 회귀 테스트를 함께 추가한다.
