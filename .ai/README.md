# AI 에이전트 및 스킬 사용 가이드

이 디렉터리는 이 저장소에서 AI 작업을 일관되게 수행하기 위한 명령, 에이전트, 스킬 문서를 모아둔 공간입니다.

## 디렉터리 구조

```text
.ai/
  commands/ # 짧게 호출하기 위한 작업 명령
  agents/   # 역할 정의: 어떤 관점으로 작업할지
  skills/   # 작업 기준: 어떤 규칙과 절차를 적용할지
```

## 명령

- `commands/code-review.md`: `/code-review`로 사용합니다. 최신 변경사항을 코드 품질, 보안, 유지보수성, 테스트, 성능 관점에서 리뷰합니다.
- `commands/plan.md`: `/plan`으로 사용합니다. 신규 기능이나 리팩터링 전 구현 계획, 백엔드 구조, API, 데이터 모델, 트레이드오프를 설계합니다.
- `commands/write-tests.md`: `/write-tests`로 사용합니다. 기능 구현 이후 필요한 Spring Boot 테스트 코드를 작성하고 Gradle 테스트와 JaCoCo 커버리지 확인을 실행합니다.

Codex에서는 이 파일들이 자동 슬래시 커맨드로 등록되지는 않습니다. 대신 `AGENTS.md`의 로컬 명령 규칙에 따라 사용자가 `/code-review`, `/plan`, `/write-tests`를 입력하면 해당 명령 파일을 읽고 따르는 방식으로 사용합니다.

## 에이전트

- `agents/plan.md`: 기능 구현 전 계획 수립, 백엔드 아키텍처 설계, 기능 분해, API/데이터 모델 결정, 확장성 및 트레이드오프 분석에 사용합니다.
- `agents/code-reviewer.md`: 코드 변경 후 품질, 보안, 유지보수성, 테스트 커버리지, 성능을 리뷰할 때 사용합니다.
- `agents/test-writer.md`: 기능 구현 후 `src/test/java` 아래에 Spring Boot 테스트 코드를 작성할 때 사용합니다.

## 스킬

- `skills/java-standard/SKILL.MD`: Java 17 코딩 표준, 네이밍, 불변성, Optional, 예외, 제네릭, 로깅, null 처리 기준입니다.
- `skills/springboot-standard/SKILL.MD`: Spring Boot 계층 구조, REST API, 검증, 서비스 트랜잭션, 예외 처리, 캐싱, 비동기 처리, 필터, 관측성 기준입니다.
- `skills/jpa-standard/SKILL.MD`: JPA/Hibernate 엔티티 설계, Repository, 트랜잭션, 쿼리 최적화, MySQL 인덱싱, 페이지네이션, 커넥션 풀, 영속성 테스트 기준입니다.
- `skills/test-guide/SKILL.MD`: JUnit 5, Mockito, MockMvc, DataJpaTest, Testcontainers, Gradle 기반의 구현 후 테스트 작성 가이드입니다.

## 권장 조합

- 기능 구현 계획 및 아키텍처 설계: `agents/plan.md` + `skills/springboot-standard/SKILL.MD` + `skills/jpa-standard/SKILL.MD` + `skills/java-standard/SKILL.MD` + `skills/test-guide/SKILL.MD`
- 코드 리뷰: `agents/code-reviewer.md` + 변경 파일에 필요한 모든 관련 스킬
- 테스트 코드 작성: `agents/test-writer.md` + `skills/test-guide/SKILL.MD` + 필요 시 `skills/jpa-standard/SKILL.MD` 또는 `skills/springboot-standard/SKILL.MD`
- JPA 중심 변경 계획: `agents/plan.md` + `skills/jpa-standard/SKILL.MD`
- 일반 Java 리팩터링: `agents/code-reviewer.md` + `skills/java-standard/SKILL.MD`

## 사용 예시

기능 구현 계획이나 아키텍처 설계가 필요할 때:

```text
/plan
booking/payment/stock 흐름을 확장 가능하게 설계해줘.
```

코드 변경 후 리뷰가 필요할 때:

```text
/code-review
```

기능 구현 후 테스트 코드를 작성해야 할 때:

```text
/write-tests
방금 구현한 예약 생성 로직에 대한 테스트 코드를 작성해줘.
```

## 기본 작업 흐름

이 저장소는 엄격한 테스트 우선 개발 흐름을 전제로 하지 않습니다. 기본 흐름은 다음과 같습니다.

1. `/plan`으로 기능 구현 또는 구조 변경 계획 수립
2. 계획에 따라 기능 구현 또는 구조 변경
3. `/write-tests`로 변경된 동작에 대한 테스트 코드 작성
4. `./gradlew test jacocoTestReport` 실행 후 JaCoCo 커버리지 확인 및 사용자에게 수치 공유
5. `/code-review`로 최신 diff 리뷰
6. 필요한 수정 후 다시 테스트 실행

테스트는 구현 이후에 작성해도 되지만, 동작이 바뀐 코드는 리뷰 또는 병합 전에 의미 있는 자동화 테스트와 JaCoCo 커버리지 확인이 있어야 합니다. 테스트를 실행한 경우 사용자에게 최신 JaCoCo 수치를 항상 공유합니다.

## 프로젝트 네이밍 규칙

- 조회 전용 도메인 컴포넌트 클래스명은 `DomainRetriever` 형식을 사용합니다. 예: `BookingRetriever`, `EventProductRetriever`
- DB 기반 조회 메서드는 `get(...)`으로 작성합니다.
- Redis 캐시 조회 메서드는 `getFromRedis(...)`로 작성합니다.
- 조회 컴포넌트에 `Reader` 네이밍을 사용하지 않습니다.

## 계층 및 예외 규칙

- application/API 쪽은 `controller`, `service`, `dto`, API-facing exception을 포함합니다.
- core 쪽은 `entity`, `component`, `repository`, core exception을 포함합니다.
- core 코드는 `BusinessException`, `ErrorCode`, HTTP 응답 정책에 의존하지 않습니다.
- core 컴포넌트는 각 도메인 `exception` 패키지의 core 예외를 던지고, service가 이를 잡아 API-facing exception으로 변환합니다.
- domain-specific exception은 `domain/{domain}/exception`에 둡니다.
- `global.exception`에는 공통 base exception, error response, global exception handler처럼 전역 응답 처리에 필요한 것만 둡니다.
- service에서 API 예외로 변환될 core 예외는 메시지/생성자 없이 빈 `final RuntimeException` 클래스로 작성해도 됩니다.

## Redis 사용 규칙

- 여러 컴포넌트에서 Redis를 사용할 때는 `RedisTemplate` 또는 `StringRedisTemplate`을 직접 흩뿌리지 않고 프로젝트 공통 Redis client/wrapper를 둡니다.
- 도메인 컴포넌트는 공통 Redis client/wrapper를 통해 value, set, lock 같은 저수준 Redis 동작을 사용합니다.
- Redis key, TTL, warm-up, lock, 장애 fallback 정책은 관련 cache/stock 컴포넌트와 결정 문서에 명시합니다.
