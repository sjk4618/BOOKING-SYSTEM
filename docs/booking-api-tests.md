# Booking API Test Notes

## Private Method Test Policy

현재 테스트는 private 메서드를 직접 호출하지 않는다. private 메서드는 구현 세부사항이므로, public 메서드의 결과와 collaborator 호출을 통해 간접 검증한다.

- `BookingFacade`의 private validation/compensation 메서드는 `createBooking(...)` 성공/실패 시나리오에서 검증한다.
- `BookingService`의 private `getBooking(...)`, `pointAmount(...)`, `requestKey(...)`는 public transactional 메서드의 상태 변경, 예외, 저장 요청을 통해 검증한다.
- `PaymentCombinationValidator`는 Spring MVC request body 검증 경로에서 검증한다.

## Assertion Style

이 테스트들은 한 테스트 메서드가 한 가지 행동을 검증하도록 작성한다. 가능한 경우 `then`에는 하나의 핵심 assertion만 둔다.

다만 예약 생성은 orchestration 성격이 강해서 예외와 보상 호출을 함께 확인해야 하는 테스트가 있다. 이런 경우에는 하나의 `then` 블록 안에 관련 검증만 묶고, unrelated 세부 호출은 검증하지 않는다.

## Test Classes

### `BookingControllerTests`

방식: `@WebMvcTest`와 `MockMvc`를 사용한 web layer 테스트.

검증 대상:

- `POST /api/bookings` 성공 시 `201 Created`와 응답 body shape를 반환한다.
- `CREDIT_CARD + Y_PAY` 조합은 request body Bean Validation 단계에서 `400 BAD_REQUEST`로 차단된다.
- 필수 헤더 누락은 `400 BAD_REQUEST`로 처리된다.
- 빈 `payments` 같은 request body 검증 실패는 `400 BAD_REQUEST`로 처리된다.

여기서는 `BookingFacade`를 mock 처리한다. controller serialization, validation, exception mapping만 검증하고 booking use case 내부 흐름은 검증하지 않는다.

### `BookingFacadeTests`

방식: Mockito 기반 단위 테스트. `BookingService`, `EventProductRetriever`, `UserValidator`, `StockReservationComponent`, `PaymentProcessorComposite`, `PaymentValidator`를 mock 처리한다.

검증 대상:

- 정상 예약 생성 시 facade가 최종 `BookingResponse`를 반환하고 확정 transactional 메서드를 호출한다.
- 예약 생성 전 결제 요청 검증 component를 호출한다.
- Redis 재고 선점 후 재고 이력과 pending payment 생성 transactional 메서드를 호출한다.
- 포인트가 아닌 결제 수단은 외부 결제 승인 component를 호출한다.
- Redis 재고 선점 결과가 `SOLD_OUT`이면 예약 취소 transactional 메서드를 호출하고 `SOLD_OUT` API 예외로 변환한다.
- 외부 결제가 실패하면 DB 보상 메서드와 Redis 재고 해제를 호출하고 `PAYMENT_FAILED` API 예외로 변환한다.
- 동일 멱등키 기존 예약이 있으면 새 상품 조회나 재고 선점 없이 기존 응답을 반환한다.

이 클래스는 use case orchestration을 검증한다. 실제 repository 접근, DB transaction, Redis 구현은 검증하지 않는다.

### `BookingServiceTests`

방식: Mockito 기반 단위 테스트. repository wrapper component와 point/history/payment component를 mock 처리한다.

검증 대상:

- pending booking 생성 결과가 `PENDING` 상태와 Redis 예약 방식으로 만들어진다.
- 재고 이력과 pending payment 저장 후 포인트를 차감한다.
- 포인트 부족 시 `POINT_NOT_ENOUGH` API 예외를 던진다.
- 결제 완료 처리 시 결제 상태와 예약 상태가 확정 상태로 바뀐다.
- 결제 실패 보상 시 포인트 복구, 결제 실패 상태 전환, 예약 실패 상태 전환, release 이력 저장이 수행된다.

이 클래스는 transactional service의 DB 상태 변경 단위를 검증한다. 실제 `@Transactional` 프록시 적용 여부는 단위 테스트 범위가 아니며, 메서드 경계가 별도 Spring bean에 존재하도록 구조를 검증 가능한 형태로 분리했다.

### `PaymentValidatorTests`

방식: 순수 단위 테스트. mock 없이 `PaymentValidator` 인스턴스를 직접 생성한다.

검증 대상:

- `CREDIT_CARD + Y_PAY` 조합은 `InvalidPaymentMethodException`으로 실패한다.
- `CREDIT_CARD + Y_POINT` 조합은 허용한다.
- `Y_PAY + Y_POINT` 조합은 허용한다.
- 결제 금액 합계가 상품 가격과 다르면 `InvalidPaymentAmountException`으로 실패한다.
- 포인트 금액이 정수로 변환될 수 없으면 `InvalidPaymentAmountException`으로 실패한다.

이 클래스는 request body 검증 이후에도 내부 호출에서 결제 정책을 방어할 수 있도록 core payment rule을 검증한다.
