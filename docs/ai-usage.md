# AI Usage Record

이 문서는 프로젝트 구현 과정에서 AI 도구를 어떤 기준과 범위로 활용했는지 정리한 기록입니다.
AI는 설계 검토, 구현 보조, 테스트 작성 보조, 문서 정리에 사용했으며, 최종 기술 선택과 요구사항 반영 여부는 코드와 문서를 기준으로 검토했습니다.

## 1. Local AI Guide

저장소의 `.ai` 디렉터리에 있는 로컬 가이드를 기준으로 작업했습니다.

```text
.ai/
  README.md
  agents/
    plan.md
    code-reviewer.md
    test-writer.md
  commands/
    plan.md
    code-review.md
    write-tests.md
  skills/
    java-standard/SKILL.MD
    springboot-standard/SKILL.MD
    jpa-standard/SKILL.MD
    test-guide/SKILL.MD
```

각 문서는 다음 역할로 사용했습니다.

```text
agents/plan.md
- booking, payment, stock, idempotency 흐름 설계
- API, transaction boundary, Redis fallback, recovery 흐름 검토
- Controller -> Service/Facade -> Component -> Repository 계층 기준 확인

agents/test-writer.md
- 구현 이후 필요한 단위 테스트 범위 정리
- JUnit 5, Mockito, MockMvc 기반 테스트 작성 기준 확인
- Gradle test와 JaCoCo 검증 흐름 확인

agents/code-reviewer.md
- 변경 후 품질, 테스트, 예외 처리, 성능 위험 검토 기준 확인
- 보안상 민감정보 포함 여부와 validation 누락 여부 점검 기준 확인

skills/java-standard/SKILL.MD
- Java 17 코드 스타일, enum/record/entity 네이밍, 예외 사용 방식 확인

skills/springboot-standard/SKILL.MD
- Spring Boot 계층 구조, request validation, exception handling, Redis wrapper 사용 기준 확인

skills/jpa-standard/SKILL.MD
- Entity 설계, transaction 분리, 조건부 update, 인덱스 기준 확인

skills/test-guide/SKILL.MD
- 테스트 범위 선정, controller/service/component 테스트 방식, JaCoCo 확인 기준 적용
```

## 2. Planning Assistance

AI는 초기 설계 단계에서 다음 쟁점을 정리하는 데 사용했습니다.

- Checkout API를 조회 전용으로 둘지, 주문서 진입 시 재고를 선점할지 비교했습니다.
- Redis Lua 기반 재고 선점과 MySQL 조건부 update fallback을 비교했습니다.
- 분산환경에서 JVM `Semaphore`가 전역 제어 수단이 될 수 없다는 점을 검토했습니다.
- Redis 장애 시 모든 요청을 DB로 보내지 않도록 circuit breaker와 fallback 전용 executor를 두는 구조를 검토했습니다.
- 멱등키를 booking table이 아니라 별도 `idempotency_keys` table로 관리하는 구조를 검토했습니다.
- 결제 수단 확장을 위해 `PaymentProcessor` 전략 패턴과 composite routing을 사용하는 구조를 검토했습니다.
- 외부 결제 호출을 DB transaction 밖으로 분리하고, 실패 시 보상 transaction을 수행하는 구조를 검토했습니다.
- 결제 완료 후 메시지 큐로 DB 반영을 비동기화하는 대안과, 현재처럼 요청 흐름에서 즉시 확정하는 방식을 비교했습니다.

최종 판단은 `DECISIONS.md`에 정리했습니다.

## 3. Implementation Assistance

AI는 다음 구현 작업을 보조하는 데 사용했습니다.

- `GET /api/checkout` API 구현
- `POST /api/bookings` API 구현
- Booking facade와 transactional service 분리
- Redis Set + Lua script 기반 재고 선점
- Redis 장애 시 MySQL 조건부 update fallback
- Redis stock circuit breaker 구현
- DB fallback 전용 executor pool 구현
- 같은 사용자가 같은 상품을 1개만 예약하도록 Redis user set과 DB active reservation 조회 추가
- `idempotency_keys` table 기반 멱등성 처리
- 같은 멱등키의 다른 request body 요청에 대한 422 처리
- 완료/실패 응답 body 저장과 replay 처리
- 결제 수단별 `PaymentProcessor` 구현과 composite routing
- `CREDIT_CARD + Y_PAY` 혼용 금지 request validation 및 service validation
- 결제 실패 시 payment, point, stock, booking 보상 처리
- 오래된 PENDING booking, PAYMENT_UNKNOWN, Redis rebuild scheduler 흐름 구현
- JPA entity 인덱스와 README DDL 정합성 반영

AI가 생성한 코드는 프로젝트의 기존 패키지 구조와 `.ai` 가이드에 맞춰 검토하며 반영했습니다.

## 4. Test Assistance

AI는 테스트 작성과 검증에도 사용했습니다.

실제 작성된 테스트 파일 기준으로 검증한 내용은 다음과 같습니다.

```text
src/test/java/booking/server/ServerApplicationTests.java
- Spring context load를 확인했습니다.

src/test/java/booking/server/domain/booking/controller/BookingControllerTests.java
- 예약 생성 성공 시 201 응답을 확인했습니다.
- 신용카드와 Y페이 혼용 request body validation 실패를 확인했습니다.
- 필수 header 누락과 request body validation 실패를 확인했습니다.
- 같은 멱등키로 다른 요청이 들어올 때 422 응답을 확인했습니다.

src/test/java/booking/server/domain/booking/service/BookingFacadeTests.java
- 재고 선점과 결제 성공 시 예약 확정 흐름을 확인했습니다.
- 예약 생성 전 결제 요청 검증 호출을 확인했습니다.
- 재고 선점 후 stock history와 pending payment 생성 호출을 확인했습니다.
- 외부 결제 승인 호출을 확인했습니다.
- 품절, 같은 사용자 중복 예약, 외부 결제 실패 보상 흐름을 확인했습니다.
- 같은 멱등키의 저장 응답 replay를 확인했습니다.

src/test/java/booking/server/domain/booking/service/BookingServiceTests.java
- PENDING booking 생성 흐름을 확인했습니다.
- stock history와 pending payment 저장, 포인트 차감을 확인했습니다.
- 이미 같은 상품을 예약한 사용자에 대한 방어를 확인했습니다.
- 포인트 부족 시 예외 흐름을 확인했습니다.
- 결제 완료와 예약 확정 상태 전환을 확인했습니다.
- 결제 실패 보상 시 포인트 복구, payment/booking 상태 전환, release 이력 저장을 확인했습니다.

src/test/java/booking/server/domain/checkout/controller/CheckoutControllerTests.java
- checkout 조회 성공 응답을 확인했습니다.
- 품절 시 409 응답을 확인했습니다.
- 상품 또는 사용자 없음 시 404 응답을 확인했습니다.
- 필수 query parameter 누락 시 400 응답을 확인했습니다.

src/test/java/booking/server/domain/checkout/service/CheckoutServiceTests.java
- 상품 cache와 Redis 재고 조회 성공 시 checkout 응답을 확인했습니다.
- 남은 재고가 없을 때 품절 예외를 확인했습니다.
- Redis 재고 조회 실패 시 예약 단계에서 확인 가능한 checkout 응답을 확인했습니다.
- 상품 없음과 사용자 없음 예외를 확인했습니다.

src/test/java/booking/server/domain/checkout/component/StockUsageRetrieverTests.java
- Redis Set size를 재고 사용량으로 변환하는 흐름을 확인했습니다.
- Redis 조회 불가 시 unavailable 결과를 확인했습니다.

src/test/java/booking/server/domain/checkout/component/UserRetrieverTests.java
- 사용자 조회 성공과 사용자 없음 예외를 확인했습니다.

src/test/java/booking/server/domain/eventproduct/component/EventProductRetrieverTests.java
- 상품 cache hit 시 repository를 조회하지 않는 흐름을 확인했습니다.
- 상품 cache miss 시 repository 조회 후 cache 저장 흐름을 확인했습니다.
- 상품 없음 예외를 확인했습니다.

src/test/java/booking/server/domain/eventproduct/component/EventProductCacheUpdaterTests.java
- transaction synchronization이 없으면 즉시 Redis cache를 갱신하는 흐름을 확인했습니다.
- transaction synchronization이 있으면 commit 이후 Redis cache를 갱신하는 흐름을 확인했습니다.

src/test/java/booking/server/domain/eventproduct/component/EventProductCacheWarmerTests.java
- warm-up lock 획득 실패 시 DB를 조회하지 않는 흐름을 확인했습니다.
- warm-up lock 획득 성공 시 DB 상품을 Redis cache에 적재하는 흐름을 확인했습니다.

src/test/java/booking/server/domain/idempotency/service/IdempotencyServiceTests.java
- 새 멱등키의 PROCESSING 생성 흐름을 확인했습니다.
- 같은 멱등키의 성공 응답 replay를 확인했습니다.
- 같은 멱등키의 다른 request body 요청에 대한 422 예외를 확인했습니다.
- 처리 중 멱등키 예외와 성공 응답 저장을 확인했습니다.

src/test/java/booking/server/domain/payment/component/PaymentValidatorTests.java
- 신용카드와 Y페이 혼용 금지를 확인했습니다.
- 신용카드와 포인트, Y페이와 포인트 조합 허용을 확인했습니다.
- 결제 금액 합계 불일치와 소수점 포인트 금액 예외를 확인했습니다.

src/test/java/booking/server/domain/payment/component/CreditCardPaymentProcessorTests.java
- 신용카드 결제 승인과 보상이 PG gateway로 위임되는지 확인했습니다.

src/test/java/booking/server/domain/payment/component/YPayPaymentProcessorTests.java
- Y페이 결제 승인과 보상이 PG gateway로 위임되는지 확인했습니다.

src/test/java/booking/server/domain/stock/component/StockReservationComponentTests.java
- Redis 선점 성공 시 Redis 예약 결과를 확인했습니다.
- Redis 선점 실패 시 전용 executor를 통한 DB fallback 예약을 확인했습니다.
- Redis circuit open 시 Redis 호출 없이 DB fallback으로 이동하는 흐름을 확인했습니다.

src/test/java/booking/server/domain/stock/component/RedisStockCircuitBreakerTests.java
- Redis 실패 임계치 도달 시 요청 차단을 확인했습니다.
- OPEN 시간 경과 후 Redis 요청 재허용을 확인했습니다.
- Redis 성공 기록 시 실패 횟수 초기화를 확인했습니다.

src/test/java/booking/server/domain/stock/component/StockFallbackExecutorTests.java
- 전용 executor에서 DB fallback 예약을 실행하는 흐름을 확인했습니다.
- executor 포화 시 예약 불가 결과를 확인했습니다.

src/test/java/booking/server/global/redis/RedisClientTests.java
- JSON value 조회와 TTL 저장을 확인했습니다.
- Redis Set size 조회를 확인했습니다.
- Redis lock 획득 성공을 확인했습니다.
```

검증에 사용한 명령은 다음과 같습니다.

```bash
./gradlew compileJava
./gradlew test
./gradlew jacocoTestCoverageVerification
```

테스트는 H2 기반 단위/슬라이스 테스트를 중심으로 작성했습니다.
실제 Redis Lua atomicity, MySQL row lock/조건부 update 경합, 분산환경 동시성은 운영 DB/Redis 또는 Testcontainers 기반 통합 테스트에서 추가로 검증하는 것이 적절하다고 판단했습니다.

## 5. Documentation Assistance

AI는 다음 문서 작성과 정리에 사용했습니다.

- `README.md`
  - 실행 방법
  - API 목록
  - sequence diagram
  - DDL
  - ERD
  - Redis key 설명

- `DECISIONS.md`
  - 재고 정합성
  - 공정성
  - Redis 장애 fallback
  - 멱등성
  - 결제 확장성
  - transaction 분리
  - stock history와 Redis rebuild
  - Checkout 조회 전용 판단
  - 메시지 큐 비동기화 대안
  - 추가 인프라와 라이브러리 판단
  - recovery strategy

- `docs/booking-payment-stock-flow.md`
  - booking, payment, stock 상세 흐름
  - Redis stock model
  - failure fallback
  - recovery 흐름

- `docs/booking-platform-fresh-design.md`
  - 요구사항 기반 전체 설계
  - API contract
  - data model
  - failure scenario
  - implementation phase

- `docs/booking-api-tests.md`
  - 테스트 클래스별 검증 대상과 방식
  - private method를 직접 테스트하지 않고 public behavior 중심으로 검증한 이유

## 6. Human Review Points

AI를 사용했지만 다음 항목은 사람이 직접 기준을 정해 검토했습니다.

- GET Checkout에서 상태를 변경하지 않는 REST 설계 판단
- Booking API에서만 재고를 확정하는 UX trade-off 수용 여부
- Redis 장애 시 availability와 DB 보호 사이의 균형
- JVM `Semaphore`를 분산환경 전역 제어 수단으로 보지 않는 판단
- 메시지 큐를 도입하지 않고 결제 완료 응답을 즉시 제공하는 UX 판단
- README와 DECISIONS에 저장소 외부 평가 맥락을 직접 드러내지 않는 문서화 기준

## 7. Limitations

현재 자동화 테스트는 단위 테스트와 Spring MVC slice 테스트 중심입니다.
다음 항목은 추가 검증 여지가 있습니다.

- Testcontainers 기반 MySQL 동시성 테스트
- Testcontainers 기반 Redis Lua atomicity 테스트
- 실제 500~1000 TPS 부하 테스트
- 여러 application server를 띄운 상태의 end-to-end fallback 테스트
- 실제 PG 연동 adapter와 reconciliation 테스트
