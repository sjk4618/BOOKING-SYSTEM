# Architecture Decisions

## Issue 1. Stock Reservation 기준

### Context

00:00 오픈 직후 짧은 시간에 500~1000 TPS가 몰릴 수 있고, 상품 재고는 10개처럼 매우 작습니다.
2대 이상의 application server가 동시에 요청을 처리하므로 JVM memory lock이나 process-local 상태로는 전역 재고 정합성을 보장할 수 없습니다.

### Options

- MySQL row lock으로만 재고를 차감합니다.
- Redis counter를 `DECR`로 차감합니다.
- Redis Set과 Lua script로 예약 ID를 원자적으로 추가합니다.

### Decision Rationale

Redis Set과 Lua script를 primary reservation path로 선택했습니다.

Redis Lua는 `SCARD`, 중복 사용자 확인, `SADD`를 하나의 원자 operation으로 처리할 수 있습니다.
여러 application server가 동시에 요청하더라도 Redis가 command 실행 순서를 하나로 직렬화하므로 전역 기준의 선착순 판단이 가능합니다.

단순 counter 방식은 빠르지만 어떤 booking이 재고를 점유했는지 알기 어렵습니다.
장애 복구나 보상에서 "누가 차지한 재고인가"가 중요하므로 bookingId Set을 사용했습니다.
또한 같은 사용자가 같은 상품을 여러 번 잡지 못하도록 userId Set도 함께 둡니다.

MySQL row lock만 사용하는 방식은 정합성은 단순하지만, 00:00 피크에 DB connection과 row lock이 병목이 될 가능성이 높습니다.
그래서 MySQL은 Redis 장애 시 제한된 fallback으로만 사용합니다.

## Issue 2. 공정성 기준

### Context

모든 사용자에게 동등한 구매 기회를 제공해야 합니다.
Checkout API에서 먼저 진입한 사용자에게 예약권을 주는 방식은 화면 진입 시점부터 상태를 만들기 때문에 abandonment, 만료, 복구 문제가 커집니다.

### Options

- Checkout 진입 시 임시 예약권을 만듭니다.
- Queue를 도입해 대기열 순서를 강제합니다.
- Booking API 도착 시점에 Redis Lua로 전역 순서를 판단합니다.

### Decision Rationale

Booking API에서만 재고를 선점하도록 했습니다.

Checkout은 상품/포인트 정보를 보여주는 조회 API로만 유지합니다.
실제 재고 확보는 결제 요청 시점의 Redis Lua 실행 순서로 결정합니다.
이 방식은 별도 queue infrastructure 없이도 여러 server에서 동일한 Redis command 순서를 기준으로 판단할 수 있습니다.

Queue는 더 강한 순서 제어를 제공하지만 운영 복잡도와 장애 처리 범위가 커집니다.
요구 traffic이 1~5분의 짧은 burst이고 재고가 매우 작기 때문에, Redis Lua 기반의 짧은 critical section이 더 단순하고 충분하다고 판단했습니다.

추가로 한 사용자가 같은 상품을 1개만 예약할 수 있도록 Redis userId Set과 DB active stock history 조회를 함께 사용합니다.
동일 사용자가 여러 탭이나 빠른 재시도로 재고를 여러 개 차지하는 상황을 줄이기 위한 선택입니다.

## Issue 3. Redis 장애와 DB Fallback

### Context

Redis를 primary stock reservation store로 사용하면 Redis 장애 시 Booking API가 모두 실패할 수 있습니다.
하지만 모든 요청을 무제한으로 DB fallback시키면 Redis 장애가 DB 장애로 전파될 수 있습니다.

### Options

- Redis 장애 시 모든 Booking 요청을 실패시킵니다.
- Redis 장애 시 모든 요청을 MySQL fallback으로 보냅니다.
- Redis circuit breaker와 fallback 전용 executor로 제한된 DB fallback을 수행합니다.

### Decision Rationale

제한된 DB fallback을 선택했습니다.

Redis 장애가 감지되면 `RedisStockCircuitBreaker`가 열리고, 일정 시간 동안 Redis reserve 호출을 건너뜁니다.
이후 DB fallback은 `stockFallbackTaskExecutor` 전용 executor pool에서만 실행합니다.
executor가 포화되거나 timeout이 발생하면 빠르게 `UNAVAILABLE`로 응답합니다.

이 구조의 목적은 availability와 blast radius 제어입니다.
Redis 장애 중에도 일부 요청은 MySQL 조건부 update로 처리할 수 있지만, request thread가 무제한으로 DB connection을 점유하지 못하게 막습니다.

DB fallback의 정합성은 다음 조건부 update가 담당합니다.

```sql
UPDATE event_products
SET used_stock = used_stock + 1
WHERE id = :eventProductId
  AND used_stock < total_stock;
```

이 update는 여러 server에서 동시에 실행되어도 `used_stock < total_stock` 조건을 만족한 요청만 성공합니다.
따라서 fallback에서도 초과 판매를 막을 수 있습니다.

초기에는 product별 `Semaphore`도 고려했지만 사용하지 않았습니다.
JVM `Semaphore`는 server 간 공유되지 않기 때문에 분산환경의 전역 보호 장치가 될 수 없습니다.
현재 구조에서 executor pool은 인스턴스별 장애 격리 장치이고, 전역 재고 정합성은 MySQL 조건부 update가 담당합니다.

## Issue 4. 멱등성 저장소

### Context

Checkout 화면에서 결제 버튼을 빠르게 여러 번 누르거나 network retry가 발생하면 같은 결제가 여러 번 요청될 수 있습니다.
중복 booking, payment, stock reservation 생성을 막아야 합니다.

### Options

- Redis에만 멱등키를 저장합니다.
- booking table에 idempotency key를 직접 둡니다.
- 별도 `idempotency_keys` table을 둡니다.

### Decision Rationale

별도 `idempotency_keys` table을 선택했습니다.

Redis-only 방식은 빠르지만 Redis 재시작이나 eviction 시 멱등성 기록이 사라질 수 있습니다.
booking table에 직접 key를 두면 성공 booking에는 단순하지만, 실패 응답이나 처리 중 상태를 표현하기 어렵습니다.

별도 table은 다음 상태를 명확히 저장할 수 있습니다.

```text
PROCESSING - 같은 key가 아직 처리 중이므로 409 응답
SUCCEEDED  - 저장된 성공 응답을 그대로 재생
FAILED     - 저장된 실패 응답을 그대로 재생
EXPIRED    - 오래된 처리 상태 정리
```

같은 idempotency key로 다른 request body가 들어오면 422로 거부합니다.
이는 client가 같은 key를 재사용하면서 다른 결제 내용을 보내는 상황을 명확한 요청 충돌로 보기 때문입니다.

응답 body도 table에 저장합니다.
완료된 요청을 재시도할 때 payment나 booking을 다시 조회해서 응답을 재구성하지 않고, 처음 완료 시 저장한 응답을 그대로 반환합니다.
이 방식은 replay 응답의 일관성을 높이고, 외부 결제나 payment 상태 재조회 의존을 줄입니다.

## Issue 5. 결제 확장성

### Context

현재 결제 수단은 `CREDIT_CARD`, `Y_PAY`, `Y_POINT`입니다.
복합 결제로 `CREDIT_CARD + Y_POINT`, `Y_PAY + Y_POINT`는 허용하지만 `CREDIT_CARD + Y_PAY`는 허용하지 않습니다.
향후 새로운 결제 수단이 추가되어도 Booking flow의 변경을 최소화해야 합니다.

### Options

- BookingFacade에서 payment method별 `if` 분기를 직접 처리합니다.
- 결제 수단별 service를 만들고 BookingFacade가 모두 의존합니다.
- `PaymentProcessor` interface와 composite routing을 사용합니다.

### Decision Rationale

전략 패턴을 선택했습니다.

각 결제 수단은 `PaymentProcessor` 구현체로 분리합니다.
`PaymentProcessorComposite`는 `PaymentMethod`에 맞는 processor를 찾아 `approve`와 `compensate`를 호출합니다.
BookingFacade는 composite만 의존하므로 결제 수단별 상세 구현을 알지 않습니다.

새 결제 수단을 추가할 때의 변경 범위는 다음으로 제한됩니다.

```text
1. PaymentMethod enum 추가
2. PaymentProcessor 구현체 추가
3. 결제 조합/금액 정책 검증 추가
```

`CREDIT_CARD + Y_PAY` 혼용 금지는 request body validation에서 1차 차단하고, `PaymentValidator`에서도 방어합니다.
request validation은 잘못된 요청을 빠르게 거부하기 위한 장치이고, service validation은 내부 호출이나 controller 우회 상황에서도 정책을 보존하기 위한 장치입니다.

## Issue 6. 외부 결제와 DB Transaction 분리

### Context

외부 PG 호출은 latency가 길고 실패 가능성이 있습니다.
DB transaction을 열어둔 상태에서 외부 API를 호출하면 connection 점유 시간이 길어지고, 00:00 burst traffic에서 DB pool 고갈로 이어질 수 있습니다.

### Options

- 하나의 transaction 안에서 booking, stock, payment, PG 호출을 모두 처리합니다.
- DB 상태 변경 transaction과 외부 결제 호출을 분리하고 보상 transaction을 둡니다.

### Decision Rationale

외부 결제 호출을 DB transaction 밖으로 분리했습니다.

BookingFacade는 use case 순서를 제어하고, BookingService는 짧은 transaction 단위를 제공합니다.
먼저 PENDING booking과 PENDING payment를 저장하고, Redis/DB stock reservation과 stock history를 남긴 뒤 외부 결제를 호출합니다.
외부 결제가 성공하면 payment를 `COMPLETED`, booking을 `CONFIRMED`로 바꿉니다.

외부 결제가 실패하면 이미 생성된 내부 상태를 보상합니다.

```text
- 승인된 외부 결제는 compensate 호출
- 포인트 결제는 users.available_point 복구
- Redis reservation은 SREM
- DB fallback reservation은 used_stock 감소
- stock_histories에는 RELEASE 이력 추가
- booking은 PAYMENT_FAILED
- payment는 FAILED 또는 CANCELLED
```

보상 중 추가 장애가 발생하면 `PAYMENT_UNKNOWN` 같은 상태를 남기고 scheduler가 재확인할 수 있게 합니다.
완벽한 분산 transaction 대신, 상태를 명시적으로 남기고 보정 가능한 구조를 선택했습니다.

## Issue 7. Stock History를 남기는 이유

### Context

Redis Set은 빠르지만 영구 저장소가 아닙니다.
Redis 재시작, key 유실, application crash 이후에도 어떤 booking이 재고를 점유했는지 복구할 수 있어야 합니다.

### Options

- Redis Set만 truth로 사용합니다.
- event_products.used_stock만 truth로 사용합니다.
- `stock_histories`를 insert-only 이력으로 남깁니다.

### Decision Rationale

Redis 정상 경로에서는 Redis Set으로 빠르게 판단하고, RDB에는 `stock_histories`를 남깁니다.

`stock_histories`는 `RESERVE`와 `RELEASE`를 insert-only로 기록합니다.
Redis가 유실되면 RELEASE 없는 RESERVE 이력을 조회해서 Redis bookingId Set과 userId Set을 재구성합니다.

`event_products.used_stock`은 Redis 장애 fallback에서만 authoritative하게 사용합니다.
Redis 정상 경로의 실시간 사용량은 Redis Set이 담당하고, 복구 기준은 stock history가 담당합니다.

이 선택은 write가 하나 더 늘어나는 비용이 있지만, 장애 복구와 보상 검증 기준이 명확해지는 이점이 더 큽니다.

## Issue 8. Checkout API를 조회 전용으로 둔 이유

### Context

Checkout API는 사용자가 주문서에 진입할 때 상품 정보, 가격, 포인트를 확인하는 API입니다.
여기서 재고를 선점하면 실제 결제하지 않은 사용자가 재고를 오래 점유할 수 있습니다.
처음에는 주문서 진입 단계부터 Redis 선점 재고를 사용해 10명만 주문서에 들어오게 하는 방식을 고려했습니다.

### Options

- Checkout 진입 시 재고를 선점합니다.
- Checkout 진입 전에 별도 POST API로 주문서 세션을 만들고 재고를 선점합니다.
- Checkout은 조회만 하고 Booking에서 재고를 선점합니다.

### Decision Rationale

Checkout은 조회 전용으로 유지했습니다.

상품 정보는 Redis cache를 우선 조회하고, 사용자 포인트는 DB에서 조회합니다.
Checkout에서 재고를 선점하지 않으므로 주문서 이탈에 따른 재고 반환 처리와 만료 관리가 단순해집니다.

가장 크게 본 지점은 HTTP method의 의미였습니다.
현재 주문서 진입 API는 GET이고, GET 요청에서 Redis 재고를 변경하거나 예약 세션을 DB에 생성하는 것은 REST 관점에서 적절하지 않다고 판단했습니다.
만약 주문서 진입을 `POST /checkout-sessions`처럼 상태 변경 API로 설계할 수 있다면, 그 시점에 Redis로 재고를 선점하고 주문서 자체에 10명만 들어오게 하는 방식도 가능합니다.

현재 방식은 사용자가 결제 정보를 입력한 뒤 Booking API에서 품절을 받을 수 있으므로 UX가 완벽하지는 않습니다.
하지만 주어진 API가 GET Checkout과 POST Booking인 상황에서는 Checkout을 조회 전용으로 두는 것이 더 일관된 설계라고 판단했습니다.
또한 주문서 화면까지는 더 많은 사용자가 동등하게 진입하고, 실제 구매 확정 경쟁은 Booking API에서 전역 Redis Lua 순서로 판단하는 방식도 공정성 관점에서 받아들일 수 있다고 봤습니다.

최종 재고 정합성은 Booking API의 Redis Lua reservation, DB fallback, stock history, idempotency table로 보장합니다.

## Issue 9. 결제 완료 후 DB 반영을 동기로 처리한 이유

### Context

결제가 완료된 뒤 booking/payment 상태를 DB에 반영하는 방식도 고민했습니다.
메시지 큐를 사용하면 외부 결제 성공 이후 DB 반영을 비동기로 넘길 수 있고, 순간 트래픽을 완충할 수 있습니다.
반면 사용자는 결제 직후 예약 완료 여부를 즉시 확인하고 싶어 합니다.

### Options

- 결제 완료 후 메시지 큐에 이벤트를 발행하고 DB 반영은 consumer가 처리합니다.
- 결제 완료 후 같은 요청 흐름에서 DB에 예약 확정 상태를 반영합니다.

### Decision Rationale

현재 구현은 결제 완료 후 같은 요청 흐름에서 DB에 예약 확정 상태를 반영합니다.

사용자 관점에서는 결제가 끝난 직후 예약이 완료되었는지 바로 확인하는 것이 자연스럽습니다.
결제는 성공했지만 예약 확정은 잠시 후 반영되는 구조는 UX상 불안감을 줄 수 있고, 그 사이 상태 조회나 재시도 처리도 더 복잡해집니다.

메시지 큐 방식은 traffic smoothing과 retry에는 유리합니다.
기획적으로 결제 완료 후 예약 확정까지 약간의 지연이 허용된다는 전제가 있다면, 결제 승인 이벤트를 queue에 넣고 consumer가 booking confirm과 payment complete를 처리하는 구조도 좋은 선택이 될 수 있습니다.

하지만 현재 범위에서는 추가 infrastructure를 늘리지 않고, 결제 결과를 즉시 응답에 반영하는 것이 더 적절하다고 판단했습니다.
대신 외부 결제 호출은 DB transaction 밖에서 수행하고, 실패나 중간 장애는 명시적인 상태와 scheduler 보정으로 다룹니다.

## Issue 10. 추가 인프라와 라이브러리 판단

### Context

기본 기술 요구사항은 Java, Spring Boot, MySQL, Redis입니다.
문제 해결을 위해 queue, message broker, distributed lock library, resilience library 등을 추가할 수 있습니다.

### Options

- Kafka/RabbitMQ 같은 queue를 도입합니다.
- Redisson 같은 Redis lock library를 도입합니다.
- Resilience4j를 도입합니다.
- Spring Boot 기본 구성과 직접 구현한 작은 component를 사용합니다.

### Decision Rationale

추가 infrastructure는 도입하지 않았습니다.

Redis Lua로 stock reservation의 원자성을 확보할 수 있고, MySQL 조건부 update로 fallback 정합성을 보장할 수 있습니다.
따라서 queue를 추가하지 않아도 요구 traffic과 제한 수량 조건을 처리할 수 있다고 판단했습니다.

Circuit breaker도 현재 필요한 범위가 Redis stock reservation 실패 감지와 fallback 차단에 한정되어 있어 작은 component로 구현했습니다.
Resilience4j는 더 많은 기능을 제공하지만, 현재 범위에서는 설정과 의존성이 늘어나는 비용이 더 크다고 판단했습니다.

Redis lock은 cache warm-up이나 cache refresh 보호에 제한적으로 사용할 수 있지만, 핵심 stock reservation은 lock이 아니라 Lua script의 원자 실행으로 처리합니다.

## Issue 11. Recovery Strategy

### Context

프로세스 crash, PG timeout, Redis 장애, 오래된 PENDING booking이 발생하면 중간 상태가 남을 수 있습니다.
중간 상태를 숨기거나 즉시 삭제하면 원인 추적과 보상이 어려워집니다.

### Options

- 실패 시 가능한 모든 상태를 즉시 rollback합니다.
- 상태를 남기고 scheduler로 보정합니다.

### Decision Rationale

상태를 명시적으로 남기고 scheduler로 보정하는 방식을 선택했습니다.

예약은 `PENDING`, `CONFIRMED`, `PAYMENT_FAILED`, `PAYMENT_UNKNOWN`, `CANCELLED`, `EXPIRED` 상태를 가집니다.
결제도 `PENDING`, `COMPLETED`, `FAILED`, `CANCELLED`, `UNKNOWN` 상태를 가집니다.

오래된 PENDING 예약은 만료 처리하고 재고를 release합니다.
PAYMENT_UNKNOWN은 reconciliation 대상이 됩니다.
Redis 재시작 이후에는 stock history 기준으로 Redis Set을 rebuild합니다.

이 방식은 상태 모델이 복잡해지는 단점이 있지만, 장애 상황에서 데이터가 사라지지 않고 복구 기준이 남는 장점이 있습니다.
