# Booking, Payment, Stock Flow

## 1. Design Summary

Checkout API는 상태를 변경하지 않는 조회 API로 둔다.
상품/가격 정보는 캐시에서 조회하고, 재고는 Redis에서 확인하며, 포인트는 `users.available_point`를 `user_id` 기준으로 단건 조회한다.

최종 재고 확보는 Booking API에서만 수행한다.
Booking API는 Redis Set을 사용해 재고 사용량을 원자적으로 선점하고, Redis 유실 복구를 위해 RDB에 `stock_histories`를 insert-only로 남긴다.

Kafka 같은 외부 queue는 사용하지 않는다.
결제 실패 보상은 동기 처리하고, 보상 실패나 오래된 PENDING 예약은 스케줄러로 보정한다.

## 1.1 GET Checkout Read Flow

Checkout 조회는 00:00 오픈 직후 트래픽이 몰리는 구간에서 DB 부하를 줄이기 위한 읽기 전용 API다.
이 API는 상품, 사용자 포인트, 현재 재고 상태를 보여주지만 예약권을 만들거나 재고를 선점하지 않는다.

조회 데이터별 기준:

```text
상품 정보        - Redis String cache 우선, miss면 DB 조회 후 Redis 저장
재고 사용량      - Redis Set의 SCARD로 확인
사용자 포인트    - users.available_point DB 단건 조회
최종 재고 선점   - Checkout이 아니라 POST Booking에서만 수행
```

Checkout 상품 정보 캐시:

```text
Key   = checkout:event-product:{eventProductId}
Value = EventProductSnapshot JSON
Type  = String
TTL   = 5~10분 + jitter
```

상품 정보는 00:00 전에 warm-up 대상으로 본다.
애플리케이션 실행 시 DB에 있는 오픈 대상 상품을 Redis에 미리 적재하고,
분산 서버 환경에서는 Redis lock을 사용해 여러 서버가 동시에 같은 warm-up을 수행하지 않게 한다.

```text
Warm-up:
1. app server가 ApplicationReadyEvent를 받는다.
2. Redis lock 획득을 시도한다.
3. lock을 획득한 서버만 오픈 대상 event_products를 DB에서 조회한다.
4. 각 상품을 checkout:event-product:{id}에 저장한다.
5. lock을 해제한다.
6. lock 획득에 실패한 서버는 warm-up을 skip한다.
```

캐시 miss 시에는 cache-aside로 DB fallback을 수행한다.
단, 00:00 트래픽에서는 동시에 많은 요청이 miss를 보고 DB로 갈 수 있으므로
필요하면 miss 구간에도 Redis lock을 둔다.

```text
상품 정보 조회:
1. GET checkout 요청
2. Redis GET checkout:event-product:{eventProductId}
3. hit면 EventProductSnapshot 반환
4. miss면 refresh lock 획득 시도
5. lock 성공 요청만 DB 조회 후 Redis SET
6. lock 실패 요청은 짧게 대기 후 Redis 재조회
7. 그래도 없으면 제한적으로 DB fallback 또는 일시 실패 응답
```

Checkout 재고 조회도 Redis를 사용한다.
여기서는 숫자 값을 `DECR`하는 방식이 아니라 Booking API와 같은 Redis Set key를 읽기만 한다.

```text
Key   = event-product:{eventProductId}:stock:used
Value = bookingId
Type  = Set
Read  = SCARD event-product:{eventProductId}:stock:used
```

Checkout 재고 계산:

```text
usedCount = SCARD event-product:{eventProductId}:stock:used
remain = eventProduct.totalStock - usedCount
```

주의할 점은 Checkout에서는 Set에 `SADD`하지 않는다는 점이다.
`SADD`는 재고 선점이므로 POST Booking에서만 Lua script로 원자 처리한다.

```text
GET Checkout:
- SCARD로 현재 사용량만 확인
- remain <= 0이면 SOLD_OUT 응답 가능
- remain > 0이면 checkout 화면 진입 허용
- 재고를 선점하지 않음

POST Booking:
- Lua script로 SCARD + SADD 원자 처리
- 성공한 경우에만 실제 재고 선점
```

Redis 장애 시 Checkout은 상품 정보와 재고 정보를 분리해서 처리한다.

```text
상품 캐시 Redis 장애:
- DB에서 상품 정보를 조회할 수 있다.
- DB 조회 성공 시 checkout 응답은 가능하다.

재고 Redis 장애:
- Checkout에서 정확한 실시간 재고를 확정하지 않는다.
- stock.status = CHECK_AT_BOOKING
- availableQuantity = null
- 사용자는 checkout에 진입할 수 있지만 POST Booking에서 최종 확인한다.
```

따라서 Checkout의 재고 응답은 사용자 경험과 불필요한 Booking 요청을 줄이기 위한 참고 정보다.
재고 정합성은 POST Booking의 Redis Set 선점, DB fallback, stock_histories로 보장한다.

## 2. Tables

### event_products

상품의 총 재고량과 DB fallback용 사용량을 가진다.

```text
id
name
price
total_stock
used_stock
check_in
check_out
open_at
created_at
updated_at
```

`used_stock`은 Redis 장애 fallback에서 MySQL 조건부 update로 사용한다.
Redis 정상 경로에서는 Redis Set과 `stock_histories`가 실시간 재고 사용량의 기준이다.

### users

회원과 사용 가능한 포인트를 가진다.

```text
id
name
available_point
created_at
updated_at
```

포인트 차감은 조건부 update로 처리한다.

```sql
UPDATE users
SET available_point = available_point - :pointAmount
WHERE id = :userId
  AND available_point >= :pointAmount;
```

### bookings

예약의 최종 상태를 가진다.

```text
id
event_product_id
user_id
status
total_amount
reserved_until
stock_reservation_method
created_at
updated_at
```

`reserved_until`은 PENDING 예약 만료 기준이다.
`stock_reservation_method`는 Redis 복구와 보상 판단에 사용한다.

```text
REDIS    - Redis Set으로 선점한 예약
DATABASE - Redis 장애 fallback에서 MySQL used_stock으로 선점한 예약
```

### payments

결제 수단별 결제 내역을 가진다.
복합 결제는 booking 하나에 payment row 여러 개로 표현한다.

```text
id
booking_id
payment_method
amount
status
request_key
pg_tx_id
created_at
updated_at
```

외부 결제 호출 전에 `PENDING` row를 먼저 생성한다.
`request_key`는 PG 요청 추적용 key다.

### stock_histories

Redis 재고 사용량 복구 기준이다.
재고 선점과 해제를 insert-only 이력으로 남긴다.

```text
id
event_product_id
booking_id
user_id
price
type
reservation_method
created_at
updated_at
```

`type`:

```text
RESERVE - 재고 사용량 증가
RELEASE - 재고 사용량 감소
```

중복 보상을 막기 위해 다음 unique key를 둔다.

```text
unique(event_product_id, booking_id, type)
```

### idempotency_keys

동일 멱등키 요청에 동일 응답을 반환하기 위한 테이블이다.

```text
id
idempotency_key
request_body
status
resource_type
resource_id
http_status
response_body
expires_at
created_at
updated_at
```

멱등키와 예약의 연결은 `idempotency_keys.resource_type = BOOKING`, `resource_id = bookings.id`로 관리한다.
`idempotency_keys.response_body`는 같은 응답을 재현하기 위한 기준이다.

## 3. Redis Stock Model

Redis에는 남은 수량 숫자가 아니라 현재 사용 중인 예약 ID Set과 사용자 ID Set을 둔다.

```text
Key   = event-product:{eventProductId}:stock:used
Value = bookingId
Type  = Set

Key   = event-product:{eventProductId}:stock:users
Value = userId
Type  = Set
```

재고 사용량:

```text
usedCount = SCARD event-product:{id}:stock:used
remain = totalStock - usedCount
```

선점은 `SCARD`와 `SADD`를 Lua script로 원자 처리한다.
`SCARD` 후 `SADD`를 별도 명령으로 처리하면 동시 요청에서 초과 판매가 발생할 수 있다.

Lua script 개념:

```lua
local used = redis.call('SCARD', KEYS[1])
local total = tonumber(ARGV[1])
local bookingId = ARGV[2]
local userId = ARGV[3]

if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
    return 2
end

if used >= total then
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

return 1
```

결과:

```text
1 - 선점 성공
0 - 품절
2 - 동일 userId로 이미 선점됨
3 - 동일 bookingId로 이미 선점됨
```

## 4. POST Booking Success Flow

Redis 정상 경로 기준이다.

```text
1. 멱등키 선점
2. Booking PENDING 생성
3. Redis Set 재고 선점
4. StockHistory RESERVE 저장
5. Payment PENDING 저장
6. 포인트 차감
7. 외부 결제 호출
8. Payment COMPLETED 저장
9. Booking CONFIRMED 저장
10. Idempotency SUCCEEDED 저장
```

## 5. Transaction Boundaries

외부 PG 호출은 DB transaction 안에서 수행하지 않는다.
PG 지연이 DB connection과 lock 점유로 이어지는 것을 막기 위해서다.

### T1. Idempotency PROCESSING 생성

```text
BEGIN
1. idempotency_keys insert
   - status = PROCESSING
   - request_body
   - expires_at
COMMIT
```

이미 같은 `idempotency_key`가 있으면 새로 처리하지 않는다.

```text
SUCCEEDED  - 저장된 http_status, response_body 반환
PROCESSING - 409 CONFLICT 또는 202 ACCEPTED
FAILED     - 정책에 따라 저장된 실패 응답 반환 또는 재시도 허용
request_body 다름 - 422 UNPROCESSABLE_ENTITY
```

### T2. Booking PENDING 생성

```text
BEGIN
1. event_products 조회
2. open_at 검증
3. 가격과 총 결제 금액 검증
4. bookings insert
   - status = PENDING
   - reserved_until = now + 3 minutes
   - stock_reservation_method = REDIS
COMMIT
```

Redis Set에는 유니크한 `bookingId`를 넣기 때문에 먼저 booking row를 생성한다.

### Redis. 재고 선점

```text
Lua script:
1. SCARD used set
2. usedCount < totalStock 확인
3. SADD bookingId
```

품절이면 booking을 `CANCELLED`로 바꾸고 멱등키에 품절 응답을 저장한다.

### T3. StockHistory RESERVE + Payment PENDING + Point 차감

```text
BEGIN
1. stock_histories insert
   - type = RESERVE
   - reservation_method = REDIS

2. payments insert
   - 결제 수단별 PENDING row 생성
   - request_key 저장

3. 포인트 결제가 있으면 users.available_point 조건부 차감

COMMIT
```

이 transaction이 실패하면 Redis `SREM`으로 재고를 해제하고 booking/idempotency를 실패 처리한다.

### External. PG 승인

```text
1. CREDIT_CARD 또는 YPAY processor 호출
2. request_key를 PG 요청 추적 키로 전달
3. timeout을 짧게 설정
```

YPOINT는 내부 포인트 차감으로 처리한다.

### T4. 성공 확정

```text
BEGIN
1. 외부 결제 payment status = COMPLETED
2. 포인트 payment status = COMPLETED
3. booking status = CONFIRMED
4. idempotency_keys status = SUCCEEDED
5. idempotency_keys response_body 저장
COMMIT
```

## 6. Failure Flow

결제 실패, PG timeout, 포인트 부족, DB 저장 실패는 보상 대상이다.

### Payment Failure Compensation

이미 Redis 선점과 stock history 저장이 끝난 뒤 결제가 실패한 경우:

```text
BEGIN
1. 포인트 차감분 복구
2. payment status = FAILED 또는 CANCELLED
3. booking status = PAYMENT_FAILED
4. stock_histories insert
   - type = RELEASE
   - reservation_method = REDIS
5. idempotency_keys status = FAILED
6. idempotency_keys response_body 저장
COMMIT
```

DB commit 후 Redis에서 재고를 해제한다.

```text
SREM event-product:{id}:stock:used {bookingId}
```

DB `RELEASE` 이력을 먼저 남기고 Redis를 해제한다.
잠시 덜 파는 것은 허용 가능하지만, Redis만 먼저 풀고 DB 이력 저장에 실패하면 복구 기준이 깨질 수 있기 때문이다.

### PG Success But Final DB Update Failure

가장 까다로운 케이스다.

```text
1. PG 승인 성공
2. T4 저장 실패
```

처리:

```text
1. PG 취소 시도
2. 취소 성공
   - payment CANCELLED
   - booking PAYMENT_FAILED
   - stock RELEASE
   - point restore
3. 취소 실패 또는 결과 불명
   - payment UNKNOWN
   - booking PENDING 유지 또는 PAYMENT_FAILED 전환 보류
   - reconciliation job 대상
```

현재 구현에서는 PG adapter가 mock이지만, 실제 장애 흐름을 고려해 `UNKNOWN` 상태를 둔다.

## 7. Redis Failure Fallback

Redis 장애 시에도 모든 Booking 요청을 DB로 무제한 우회시키지는 않는다.
Redis 장애를 DB 장애로 전파시킬 수 있기 때문이다.

Fallback은 MySQL 조건부 update를 사용한다.

```sql
UPDATE event_products
SET used_stock = used_stock + 1
WHERE id = :eventProductId
  AND used_stock < total_stock;
```

영향 row:

```text
1 - DB 기준 재고 선점 성공
0 - 품절
```

Redis 장애 fallback transaction:

```text
BEGIN
1. event_products used_stock 조건부 증가
2. stock_histories insert
   - type = RESERVE
   - reservation_method = DATABASE
3. payments PENDING insert
4. 포인트 차감
COMMIT
```

결제 성공:

```text
BEGIN
1. payment COMPLETED
2. booking CONFIRMED
3. idempotency SUCCEEDED
COMMIT
```

결제 실패:

```text
BEGIN
1. event_products used_stock = used_stock - 1
2. stock_histories insert
   - type = RELEASE
   - reservation_method = DATABASE
3. 포인트 복구
4. payment FAILED/CANCELLED
5. booking PAYMENT_FAILED
6. idempotency FAILED
COMMIT
```

현재 구현의 DB fallback 경로는 `RedisStockCircuitBreaker`와 `stockFallbackTaskExecutor`로 보호한다.

```text
RedisStockCircuitBreaker
- Redis reserve 실패가 3회 누적되면 OPEN
- OPEN 동안 Redis reserve를 건너뛰고 DB fallback으로 바로 이동
- 10초 후 HALF_OPEN으로 일부 요청을 Redis에 다시 통과
- 성공하면 CLOSED, 실패하면 다시 OPEN

stockFallbackTaskExecutor
- corePoolSize = 4
- maxPoolSize = 4
- queueCapacity = 20
- fallback timeout = 2초
```

이 전용 executor pool은 DB fallback이 request thread에서 무제한으로 DB connection을 점유하지 않도록 하는 인스턴스별 장애 격리 장치다.
전역 재고 정합성은 executor가 아니라 MySQL 조건부 update가 담당한다.
분산환경에서 JVM `Semaphore`는 서버 간 공유되지 않으므로 fallback 보호 장치로 사용하지 않는다.

## 8. Redis Recovery

Redis가 재시작되면 기존 Set key가 없거나 틀릴 수 있다.
Redis 값을 단독 truth로 쓰지 않고, `stock_histories` 기준으로 Redis Set을 재구성한다.

복구 순서:

```text
1. Redis circuit breaker를 아직 OPEN 상태로 둔다.
2. 신규 Redis 선점을 잠시 중단한다.
3. 오래된 PENDING booking을 만료/보상한다.
4. stock_histories에서 RELEASE 없는 booking_id를 조회한다.
5. Redis 예약 ID Set과 사용자 ID Set을 삭제 후 다시 SADD 한다.
6. event_products.used_stock과 history 기준 사용량이 다르면 보정한다.
7. Redis health check 성공 후 circuit breaker를 CLOSED로 돌린다.
```

현재 사용 중인 재고 조회 SQL 개념:

```sql
SELECT booking_id
FROM stock_histories
WHERE event_product_id = :eventProductId
GROUP BY booking_id
HAVING
    SUM(CASE WHEN type = 'RESERVE' THEN 1 ELSE 0 END) > 0
    AND SUM(CASE WHEN type = 'RELEASE' THEN 1 ELSE 0 END) = 0;
```

조회된 `booking_id`를 Redis 예약 ID Set에 다시 넣고, 같은 기준의 `user_id`를 Redis 사용자 ID Set에 다시 넣는다.

```text
SADD event-product:{id}:stock:used {bookingId1} {bookingId2} ...
SADD event-product:{id}:stock:users {userId1} {userId2} ...
```

## 9. Why No Checkout Session

Checkout은 구매권 발급이 아니라 정보 조회로 정의한다.
따라서 `pre_booking_session` 테이블은 사용하지 않는다.

Checkout에서 재고를 선점하지 않기 때문에 다음 복잡도를 피할 수 있다.

```text
- GET API의 상태 변경
- 주문서 이탈자의 재고 점유
- session TTL 만료 처리
- session 기준 Redis 복구
```

최종 정합성은 Booking API의 Redis 선점, stock history, idempotency table, payment pending row로 보장한다.

## 10. Key Trade-Offs

장점:

```text
- Checkout은 상태 변경 없는 조회 API로 유지된다.
- Redis 장애/복구 시 stock_histories로 Redis Set을 재구성할 수 있다.
- 멱등성 테이블로 동일 요청에 동일 응답을 반환할 수 있다.
- 외부 PG 호출이 DB transaction을 오래 점유하지 않는다.
```

단점:

```text
- Booking PENDING 생성 후 Redis 선점 실패 시 취소 상태가 남는다.
- Redis 정상 경로와 DB fallback 경로를 모두 고려해야 한다.
- stock_histories insert와 Redis SADD/SREM 간 보상 로직이 필요하다.
```
