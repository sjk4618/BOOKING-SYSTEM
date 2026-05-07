# Booking Platform Fresh Design

이 문서는 `docs/requirements.md`만 기준으로 다시 설계한 안이다.
현재 구현 상태나 이전 설계 문서는 반영하지 않고, 제한 수량 숙박 상품을 00:00 오픈 시점에 안전하게 판매하기 위한 구조를 제안한다.

## 1. Design Goals

- Checkout API는 조회 전용으로 유지한다.
- Booking API만 재고, 결제, 포인트, 예약 상태를 변경한다.
- 2대 이상 애플리케이션 서버에서 동일한 재고 정합성을 보장한다.
- 500~1000 TPS 피크에서 DB connection과 row lock이 먼저 고갈되지 않게 한다.
- 외부 PG 호출은 DB transaction 밖에서 수행한다.
- 멱등키는 중복 booking, payment, stock reservation 생성을 막는 1차 방어선으로 둔다.
- 결제 수단 추가 시 Booking API orchestration 코드는 거의 바뀌지 않게 한다.
- Redis 장애, PG 장애, 프로세스 crash, 오래된 PENDING 상태를 복구 가능한 상태로 남긴다.

## 2. High Level Architecture

```text
Client
  -> Controller
  -> Application Facade
  -> Transactional Application Services
  -> Domain Components
  -> Repository / RedisClient / PaymentGateway
```

계층 책임:

```text
Controller
- Header, request body Bean Validation
- HTTP status, response body 반환
- 인증은 요구사항 범위 밖이므로 userId header만 받는다.

Application Facade
- Booking use case 전체 순서 제어
- transaction을 오래 열지 않는다.
- Redis 선점, DB transaction, 외부 결제 호출, 보상 호출을 조합한다.
- core exception을 API-facing exception으로 변환한다.

Transactional Application Services
- 짧은 DB transaction 단위를 제공한다.
- idempotency 시작/성공/실패 저장
- booking pending/confirm/fail/cancel 상태 변경
- payment pending/complete/fail/cancel 상태 변경
- point deduct/restore
- stock history reserve/release 저장

Domain Components
- 조회, 저장, 검증, 정책, Redis 연산, PG adapter를 담당한다.
- repository를 직접 의존해도 되지만 BusinessException, ErrorCode, HTTP 정책에는 의존하지 않는다.

Repository
- JPA persistence access only
- 조건부 update, unique constraint, projection query 제공
```

추천 패키지:

```text
domain/checkout/controller
domain/checkout/service
domain/checkout/dto

domain/booking/controller
domain/booking/service
domain/booking/component
domain/booking/domain/entity
domain/booking/repository
domain/booking/dto
domain/booking/exception

domain/payment/component
domain/payment/domain/entity
domain/payment/repository
domain/payment/exception

domain/stock/component
domain/stock/domain/entity
domain/stock/repository
domain/stock/exception

domain/idempotency/component
domain/idempotency/domain/entity
domain/idempotency/repository
domain/idempotency/exception

global/redis
global/exception
```

## 3. API Contract

### 3.1 GET Checkout

```text
GET /api/checkout?eventProductId={eventProductId}
Header:
  userId: Long

Response:
{
  "eventProductId": 1,
  "name": "Special Lodging",
  "price": 100000,
  "checkInAt": "2026-06-01T15:00:00",
  "checkOutAt": "2026-06-02T11:00:00",
  "openAt": "2026-06-01T00:00:00",
  "stock": {
    "status": "AVAILABLE | SOLD_OUT | CHECK_AT_BOOKING",
    "remainingQuantity": 7
  },
  "user": {
    "userId": 10,
    "name": "user",
    "availablePoint": 30000
  }
}
```

규칙:

- 상태 변경 없음.
- 상품 정보는 Redis cache 우선, cache miss면 DB fallback.
- 포인트는 DB 단건 조회.
- Redis 재고 조회 실패 시 checkout은 실패시키지 않고 `CHECK_AT_BOOKING`으로 응답한다.
- 최종 재고 확보는 Booking API에서만 수행한다.

### 3.2 POST Booking

```text
POST /api/bookings
Header:
  userId: Long
  Idempotency-Key: NotBlank String

Request:
{
  "eventProductId": 1,
  "payments": [
    {"paymentMethod": "CREDIT_CARD", "amount": 90000},
    {"paymentMethod": "Y_POINT", "amount": 10000}
  ]
}

Response:
{
  "bookingId": 100,
  "eventProductId": 1,
  "userId": 10,
  "status": "CONFIRMED",
  "totalAmount": 100000,
  "reservedUntil": "2026-06-01T00:03:00",
  "payments": [
    {"paymentId": 1, "paymentMethod": "CREDIT_CARD", "amount": 90000, "status": "COMPLETED"},
    {"paymentId": 2, "paymentMethod": "Y_POINT", "amount": 10000, "status": "COMPLETED"}
  ]
}
```

Request validation:

- `userId` header required.
- `Idempotency-Key` header `@NotBlank`.
- `eventProductId` required.
- `payments` required and non-empty.
- `paymentMethod` required.
- `amount` positive.
- `CREDIT_CARD + Y_PAY` combination rejected at request body validation.
- total payment amount must equal product price in service policy validation.
- point amount must be integer.

## 4. Data Model

### event_products

```text
id bigint pk
name varchar(200) not null
price decimal(19, 0) not null
total_stock int not null
used_stock int not null default 0
check_in_at datetime not null
check_out_at datetime not null
open_at datetime not null
created_at datetime not null
updated_at datetime not null

index idx_event_products_open_at(open_at)
```

`used_stock`은 Redis 장애 fallback에서만 authoritative하게 사용한다.
Redis 정상 경로에서는 Redis used set과 `stock_histories`가 재고 복구 기준이다.

### users

```text
id bigint pk
name varchar(100) not null
available_point int not null
created_at datetime not null
updated_at datetime not null
```

포인트 차감:

```sql
UPDATE users
SET available_point = available_point - :pointAmount
WHERE id = :userId
  AND available_point >= :pointAmount;
```

### idempotency_keys

```text
id bigint pk
idempotency_key varchar(100) not null
request_body json not null
status varchar(30) not null
resource_type varchar(30)
resource_id bigint
http_status int
response_body json
expires_at datetime not null
created_at datetime not null
updated_at datetime not null

unique uk_idempotency_key(idempotency_key)
index idx_idempotency_expires_at(expires_at)
```

상태:

```text
PROCESSING
SUCCEEDED
FAILED
EXPIRED
```

동작:

- 같은 key + 같은 request body + `SUCCEEDED`: 저장된 응답 반환.
- 같은 key + 같은 request body + `PROCESSING`: `409 IDEMPOTENCY_PROCESSING` 또는 `202 ACCEPTED`.
- 같은 key + 다른 request body: `422 IDEMPOTENCY_REQUEST_MISMATCH`.
- `FAILED`: 같은 실패 응답 반환을 기본으로 한다. PG timeout 같은 불명 상태는 재시도 허용하지 않는다.
- 성공한 예약과의 연결은 `resource_type = BOOKING`, `resource_id = bookings.id`로 관리한다.

### bookings

```text
id bigint pk
event_product_id bigint not null
user_id bigint not null
status varchar(30) not null
total_amount decimal(19, 0) not null
reserved_until datetime not null
stock_reservation_method varchar(30) not null
created_at datetime not null
updated_at datetime not null

index idx_bookings_status_reserved_until(status, reserved_until)
index idx_bookings_event_product_id(event_product_id)
```

상태:

```text
PENDING
CONFIRMED
CANCELLED
PAYMENT_FAILED
PAYMENT_UNKNOWN
EXPIRED
```

`PAYMENT_UNKNOWN`은 PG 승인 성공 여부 또는 PG 취소 성공 여부를 확정하지 못한 상태다.
운영 reconciliation job의 대상이다.

### payments

```text
id bigint pk
booking_id bigint not null
payment_method varchar(30) not null
amount decimal(19, 0) not null
status varchar(30) not null
request_key varchar(120) not null
pg_tx_id varchar(120)
failure_reason varchar(500)
created_at datetime not null
updated_at datetime not null

unique uk_payments_request_key(request_key)
index idx_payments_booking_id(booking_id)
```

상태:

```text
PENDING
COMPLETED
FAILED
CANCELLED
UNKNOWN
```

복합 결제는 booking 하나에 여러 payment row로 표현한다.

### stock_histories

```text
id bigint pk
event_product_id bigint not null
booking_id bigint not null
user_id bigint not null
type varchar(30) not null
reservation_method varchar(30) not null
created_at datetime not null

unique uk_stock_histories_booking_type(booking_id, type)
index idx_stock_histories_event_product_id(event_product_id)
```

상태:

```text
RESERVE
RELEASE
```

`stock_histories`는 insert-only event log다.
Redis 복구와 보상 중복 방지의 기준으로 사용한다.

## 5. Redis Model

### Product cache

```text
Key   = checkout:event-product:{eventProductId}
Type  = String JSON
TTL   = 5~10 minutes + jitter
```

상품 cache는 00:00 전에 warm-up한다.
여러 app server가 동시에 warm-up하지 않도록 Redis lock을 사용한다.

### Stock used set

```text
Key    = event-product:{eventProductId}:stock:used
Type   = Set
Member = bookingId

Key    = event-product:{eventProductId}:stock:users
Type   = Set
Member = userId
```

예약 선점은 Lua script로 재고 수량과 사용자 중복 여부를 원자 처리한다.

```lua
local used = redis.call('SCARD', KEYS[1])
local total = tonumber(ARGV[1])
local bookingId = ARGV[2]
local userId = ARGV[3]
local soldOutTtlSeconds = tonumber(ARGV[4])

if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
    return 2
end

if used >= total then
    redis.call('SET', KEYS[3], '1', 'EX', soldOutTtlSeconds)
    return 0
end

local added = redis.call('SADD', KEYS[1], bookingId)

if added == 0 then
    return 3
end

local userAdded = redis.call('SADD', KEYS[2], userId)

if userAdded == 0 then
    redis.call('SREM', KEYS[1], bookingId)
    return 2
end

if used + 1 >= total then
    redis.call('SET', KEYS[3], '1', 'EX', soldOutTtlSeconds)
end

return 1
```

결과:

```text
1 = RESERVED
0 = SOLD_OUT
2 = ALREADY_RESERVED
3 = DUPLICATED_BOOKING_ID
```

### Sold out marker

```text
Key  = event-product:{eventProductId}:stock:sold-out
Type = String
TTL  = short, for example 30 seconds
```

목적:

- 재고가 이미 소진된 뒤 모든 요청이 DB idempotency insert까지 도달하는 것을 막는다.
- marker가 잘못 남아도 짧은 TTL 후 Redis set 기준으로 다시 판단한다.
- marker는 최적화이며 정합성의 기준이 아니다.

## 6. Booking Flow

### Success path

```text
1. Controller validation
2. Product cache 조회
3. open_at 검증
4. payment combination, amount policy 검증
5. sold-out marker fast check
6. T1: idempotency PROCESSING insert or existing key 판단
7. T2: booking PENDING insert
8. Redis Lua stock reserve with bookingId
9. T3: stock history RESERVE, payment PENDING, point deduct
10. External PG approve
11. T4: payment COMPLETED, booking CONFIRMED, idempotency SUCCEEDED with response body
12. 201 response
```

T1과 T2를 분리하는 이유:

- idempotency conflict는 booking 생성 전에 판단한다.
- booking row id가 있어야 Redis used set member를 bookingId로 저장할 수 있다.
- transaction을 짧게 유지해 피크 구간 DB connection 점유 시간을 줄인다.

### T1. Idempotency start

```text
BEGIN
1. idempotency_keys insert
   - idempotency_key
   - request_body
   - status = PROCESSING
   - expires_at = now + 24 hours
COMMIT
```

중복 key면 existing row를 조회해 상태별로 처리한다.

### T2. Booking pending

```text
BEGIN
1. bookings insert
   - status = PENDING
   - reserved_until = now + 3 minutes
   - stock_reservation_method = REDIS
COMMIT
```

### Redis stock reserve

```text
1. Lua script 실행
2. SOLD_OUT이면 T2 booking을 CANCELLED로 바꾸고 idempotency FAILED response 저장
3. RESERVED 또는 DUPLICATED면 다음 단계 진행
4. Redis 장애면 fallback 정책으로 이동
```

### T3. Reserve history, payments, point

```text
BEGIN
1. stock_histories RESERVE insert
2. payment method별 payments PENDING insert
3. Y_POINT 금액이 있으면 users.available_point 조건부 차감
COMMIT
```

이 단계 실패 시:

```text
1. T_compensate_db: booking PAYMENT_FAILED, payment FAILED, stock history RELEASE 가능하면 저장
2. Redis SREM bookingId
3. idempotency FAILED response 저장
```

### External payment

```text
1. CREDIT_CARD, Y_PAY만 외부 adapter 호출
2. Y_POINT는 T3의 point deduct로 승인 완료로 본다.
3. request_key = booking:{bookingId}:{paymentMethod}
4. timeout은 짧게 설정한다.
5. retry는 PG idempotency를 지원하는 request_key 기반으로만 수행한다.
```

### T4. Confirm

```text
BEGIN
1. payments COMPLETED
2. booking CONFIRMED
3. idempotency SUCCEEDED
4. http_status, response_body 저장
COMMIT
```

## 7. Failure Handling

### Redis reserve sold out

```text
1. booking CANCELLED
2. idempotency FAILED with SOLD_OUT response
3. payment row는 만들지 않는다.
```

### Redis failure

Redis 장애 시 모든 요청을 DB fallback으로 보내지 않는다.
fallback은 circuit breaker와 작은 concurrency limit으로 보호한다.

정상 Redis 경로:

```text
Redis CLOSED -> Lua reserve 사용
```

장애 감지:

```text
Redis timeout/error rate threshold 초과 -> circuit OPEN
```

DB fallback:

```sql
UPDATE event_products
SET used_stock = used_stock + 1
WHERE id = :eventProductId
  AND used_stock < total_stock;
```

fallback 성공 시 `stock_reservation_method = DATABASE`로 booking을 유지한다.
fallback 실패 시 SOLD_OUT 또는 STOCK_RESERVATION_FAILED로 응답한다.

DB fallback은 다음 보호 장치를 둔다.

```text
- RedisStockCircuitBreaker
- stockFallbackTaskExecutor 전용 executor pool
- fallback timeout 2초
- 실패 시 빠른 409/503 계열 응답
```

현재 구현에서는 JVM `Semaphore`를 사용하지 않는다.
분산 서버 환경에서 `Semaphore`는 서버 간 공유되지 않으므로 전역 보호 장치가 될 수 없다.
전역 재고 정합성은 MySQL 조건부 update가 담당하고, fallback 전용 executor는 인스턴스별 장애 격리와 DB fallback 동시성 제한을 담당한다.

### Payment failure

PG 실패 또는 한도 초과:

```text
BEGIN
1. point restore
2. payment FAILED
3. booking PAYMENT_FAILED
4. stock_histories RELEASE insert
5. idempotency FAILED response 저장
COMMIT

After commit:
6. Redis SREM bookingId
```

DB release history를 먼저 남기고 Redis를 해제한다.
잠시 덜 파는 것은 허용 가능하지만, Redis만 먼저 풀고 DB 이력 저장에 실패하면 복구 기준이 깨진다.

### PG success but final DB confirm failure

```text
1. PG 승인 성공
2. T4 confirm 실패
```

처리:

```text
1. PG cancel 시도
2. cancel 성공이면 payment CANCELLED, booking PAYMENT_FAILED, point restore, stock RELEASE
3. cancel 실패 또는 결과 불명이면 payment UNKNOWN, booking PAYMENT_UNKNOWN
4. reconciliation job에서 PG 조회 후 확정 또는 취소
```

### Process crash

Crash 지점별 복구:

```text
After idempotency PROCESSING, before booking:
- idempotency expires_at 기준 EXPIRED 처리

After booking PENDING, before Redis reserve:
- reserved_until 기준 booking EXPIRED/CANCELLED

After Redis reserve, before stock history:
- booking PENDING인데 stock history RESERVE가 없으면 Redis SREM 후 booking EXPIRED

After point deduct, before PG:
- reserved_until 초과 시 point restore, payment FAILED, stock RELEASE

After PG approve, before confirm:
- payment UNKNOWN 또는 PG reconciliation 대상으로 전환
```

## 8. Recovery Jobs

### Pending booking expiry job

주기:

```text
every 10~30 seconds
```

대상:

```sql
SELECT *
FROM bookings
WHERE status = 'PENDING'
  AND reserved_until < NOW()
LIMIT 100;
```

처리:

```text
1. booking row를 짧은 transaction에서 lock
2. payment 상태와 PG 여부 확인
3. PG 미호출 또는 실패면 RELEASE, point restore, booking EXPIRED/PAYMENT_FAILED
4. Redis SREM 또는 DB used_stock decrement
5. idempotency FAILED/EXPIRED 저장
```

### Redis rebuild job

Redis 장애 후 회복 시:

```text
1. circuit OPEN 유지
2. 신규 Redis reserve 중단
3. expired pending booking 먼저 정리
4. stock_histories 기준 active bookingId 조회
5. Redis used set 삭제 후 재구성
6. event_products.used_stock 보정
7. circuit HALF_OPEN 후 CLOSED
```

active stock query:

```sql
SELECT booking_id
FROM stock_histories
WHERE event_product_id = :eventProductId
GROUP BY booking_id
HAVING
    SUM(CASE WHEN type = 'RESERVE' THEN 1 ELSE 0 END) > 0
    AND SUM(CASE WHEN type = 'RELEASE' THEN 1 ELSE 0 END) = 0;
```

### Payment reconciliation job

대상:

```text
payment.status = UNKNOWN
booking.status = PAYMENT_UNKNOWN
```

처리:

```text
1. PG transaction 조회
2. 승인 성공이면 booking CONFIRMED, payment COMPLETED
3. 승인 실패/취소면 point restore, stock RELEASE, booking PAYMENT_FAILED
4. 결과 불명이면 다음 cycle로 유지하되 운영 alert 발생
```

## 9. Payment Extensibility

핵심은 Booking flow가 결제 수단별 분기문을 직접 갖지 않게 하는 것이다.

```text
PaymentPolicy
- 결제 수단 조합 검증
- 총액 검증
- point 정수 검증

PaymentProcessor
- supports(paymentMethod)
- prepare(...)
- approve(...)
- complete(...)
- compensate(...)

PaymentGateway
- supports(paymentMethod)
- approve(...)
- compensate(...)
- 실제 PG 연동을 담당하는 adapter interface

PaymentProcessorComposite
- request의 paymentMethod별 processor 선택
- 외부 결제는 approve 호출
- 내부 포인트는 prepare 단계에서 차감
```

결제 수단 추가 절차:

```text
1. PaymentMethod enum 추가
2. PaymentProcessor 구현체 추가
3. 외부 승인이 필요한 수단이면 PaymentGateway 구현체 추가
4. PaymentPolicy에 조합 허용 여부 추가
5. 테스트 케이스 추가
```

Booking Facade는 `PaymentProcessorComposite`만 호출하므로 새 결제 수단이 생겨도 flow 변경을 최소화한다.

조합 정책:

```text
Allowed:
- CREDIT_CARD
- Y_PAY
- Y_POINT
- CREDIT_CARD + Y_POINT
- Y_PAY + Y_POINT

Rejected:
- CREDIT_CARD + Y_PAY
- duplicate same payment method
```

## 10. Transaction Boundary Summary

```text
T1 idempotency start
- DB only
- short transaction

T2 booking pending
- DB only
- short transaction

Redis reserve
- Redis Lua
- no DB transaction

T3 reserve history/payment pending/point deduct
- DB only
- short transaction

External PG approve
- external network call
- no DB transaction

T4 confirm
- DB only
- short transaction

T_compensate
- DB only
- short transaction

Redis release
- after DB release history commit
```

같은 클래스 내부 private method 호출로 transaction을 나누지 않는다.
트랜잭션 단위는 별도 Spring bean의 public method로 제공한다.

## 11. Error Policy

```text
400 BAD_REQUEST
- request body/header validation failure
- payment amount mismatch
- malformed enum/json

404 NOT_FOUND
- product not found
- user not found
- booking not found

409 CONFLICT
- sold out
- booking not open
- idempotency processing
- invalid payment method combination
- point not enough
- stock reservation failed
- payment failed

422 UNPROCESSABLE_ENTITY
- same idempotency key with different request body

500 INTERNAL_SERVER_ERROR
- unhandled system error
```

Controller validation error는 `GlobalExceptionHandler`에서 예외별로 `BAD_REQUEST`로 변환한다.
Service는 core exception을 잡아 API-facing exception으로 변환한다.
Core component는 `BusinessException`, `BadRequestException`, `ErrorCode`에 의존하지 않는다.

## 12. Fairness and Load Protection

공정성 기준:

```text
- app server별 local counter, local lock, local queue를 쓰지 않는다.
- 모든 서버가 같은 Redis Lua script로 재고 선점을 시도한다.
- Redis가 처리한 command 순서가 전역 선착순 기준이 된다.
- DB fallback도 MySQL 조건부 update 하나로 전역 기준을 둔다.
```

부하 보호:

```text
- Checkout 상품 정보 cache warm-up
- Checkout은 상태 변경 금지
- sold-out marker로 소진 후 요청 fast reject
- 외부 PG 호출은 재고 선점 성공자만 수행
- DB fallback concurrency limit
- Hikari pool size와 timeout을 보수적으로 설정
- request timeout을 짧게 설정
```

선택하지 않을 것:

```text
- Kafka 대기열: 10개 재고와 1000 TPS 수준에서는 운영 복잡도 대비 이득이 작다.
- DB pessimistic lock 기반 선착순: 피크 구간에서 lock wait와 connection 고갈 위험이 크다.
- Checkout session 선점: 조회 API가 상태를 변경하고 이탈 사용자의 재고 점유 문제가 생긴다.
```

## 13. Implementation Order

### Phase 1. Domain model and persistence

```text
1. event_products, users, bookings, payments, stock_histories, idempotency_keys entity/repository 작성
2. unique/index 제약 추가
3. core exception 정의
4. repository query와 조건부 update 테스트
```

### Phase 2. Read model and checkout

```text
1. EventProductRetriever + Redis cache 구현
2. cache warm-up 구현
3. StockUsageRetriever 구현
4. CheckoutService/Controller 구현
5. Redis 장애 시 CHECK_AT_BOOKING 응답 처리
```

### Phase 3. Idempotency and stock reservation

```text
1. IdempotencyService 구현
2. RedisClient Lua reserve/release 구현
3. StockReservationComponent 구현
4. DB fallback conditional update 구현
5. sold-out marker 구현
```

### Phase 4. Booking transaction services

```text
1. BookingTransactionService 구현
2. PaymentTransactionService 구현
3. StockHistoryService 구현
4. PointService 구현
5. 각 transaction boundary public method 분리
```

### Phase 5. Payment abstraction

```text
1. PaymentPolicy 구현
2. PaymentProcessor interface 구현
3. CreditCardProcessor, YPayProcessor, YPointProcessor 구현
4. PaymentProcessorComposite 구현
5. PG mock adapter 구현
```

### Phase 6. Booking facade

```text
1. POST Booking orchestration 구현
2. 성공/실패 보상 흐름 구현
3. idempotency response 저장 구현
4. API exception mapping 구현
```

### Phase 7. Recovery jobs

```text
1. Pending booking expiry job
2. Redis rebuild job
3. Payment reconciliation job
4. 운영 로그와 metric 추가
```

### Phase 8. Documentation

```text
1. README architecture/API/sequence/ERD 작성
2. DECISIONS.md에 Redis, DB fallback, payment abstraction, idempotency, recovery trade-off 작성
3. local infra 실행 방법 작성
```

## 14. Test Strategy

Unit tests:

```text
- PaymentPolicy: allowed/rejected combinations, amount mismatch, point integer
- PaymentProcessorComposite: processor routing, missing processor failure
- BookingFacade: success path, sold out, point failure, PG failure, idempotency replay
- Transaction services: status transition, point deduct/restore, payment status update
- StockReservationComponent: Redis result mapping, fallback decision
```

Web tests:

```text
- Checkout response shape
- Booking request validation
- Idempotency-Key blank/missing
- CREDIT_CARD + Y_PAY request body rejection
- exception handler status/code mapping
```

Persistence tests:

```text
- idempotency unique key conflict
- booking idempotency unique key
- stock history duplicate RESERVE/RELEASE prevention
- user point conditional update
- event product used_stock conditional update
```

Integration tests:

```text
- concurrent booking attempts never exceed total_stock
- repeated same idempotency key creates one booking
- payment failure restores point and stock
- Redis down fallback path
- expired pending booking recovery
```

Concurrency tests should use Testcontainers MySQL and Redis when possible.
Mockito unit tests are not enough to prove Lua atomicity, MySQL conditional update, unique constraint, or transaction isolation.

## 15. DECISIONS.md Candidates

### Issue 1. Redis Lua vs DB lock for stock

Decision:

```text
Use Redis Lua as primary stock reservation path.
Use MySQL conditional update only as bounded fallback.
```

Rationale:

```text
Redis Lua gives one atomic global decision with low latency.
DB lock based reservation is simpler but risks lock wait and connection exhaustion at 00:00.
MySQL fallback preserves availability during Redis outage but must be limited to protect DB.
```

### Issue 2. No checkout session

Decision:

```text
Checkout remains read-only.
Only Booking reserves stock.
```

Rationale:

```text
Checkout session would improve perceived fairness but introduces state mutation, abandonment cleanup, and extra recovery complexity.
The requirements only need checkout information and final booking consistency.
```

### Issue 3. External payment outside DB transaction

Decision:

```text
Do not call PG inside a DB transaction.
```

Rationale:

```text
PG latency must not hold DB connection or locks.
The cost is explicit compensation logic, which is required anyway by the failure handling requirements.
```

### Issue 4. Idempotency table

Decision:

```text
Use MySQL idempotency_keys as authoritative idempotency store.
Redis sold-out marker is only optimization.
```

Rationale:

```text
Redis-only idempotency is fast but weak during Redis loss/restart.
MySQL gives durable replay and request body conflict detection.
```

### Issue 5. Payment extension

Decision:

```text
Use PaymentPolicy and PaymentProcessor strategy.
Booking flow depends on the composite only.
```

Rationale:

```text
Payment method branching is isolated from booking orchestration.
Adding a method should add a processor and policy entry, not rewrite the use case.
```
