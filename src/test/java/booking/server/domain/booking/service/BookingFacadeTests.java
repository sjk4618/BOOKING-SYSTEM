package booking.server.domain.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import booking.server.domain.booking.dto.BookingCreateResult;
import booking.server.domain.booking.dto.BookingCreateRequest;
import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.booking.dto.BookingPaymentResponse;
import booking.server.domain.booking.dto.BookingResponse;
import booking.server.domain.eventproduct.component.EventProductRetriever;
import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.idempotency.service.IdempotencyCurrentPosition;
import booking.server.domain.idempotency.service.IdempotencyService;
import booking.server.domain.payment.component.PaymentProcessorComposite;
import booking.server.domain.payment.component.PaymentValidator;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import booking.server.domain.stock.component.StockReservation;
import booking.server.domain.stock.component.StockReservationComponent;
import booking.server.domain.stock.component.StockReservationResult;
import booking.server.domain.stock.domain.entity.StockReservationMethod;
import booking.server.domain.user.component.UserValidator;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingFacadeTests {

	private static final long USER_ID = 10L;
	private static final long EVENT_PRODUCT_ID = 1L;
	private static final long BOOKING_ID = 100L;
	private static final String IDEMPOTENCY_KEY = "booking-key-1";
	private static final String CARD_REQUEST_BODY = "userId=10|eventProductId=1|payments=[CREDIT_CARD=100000]";

	@Mock
	private BookingService bookingService;

	@Mock
	private EventProductRetriever eventProductRetriever;

	@Mock
	private UserValidator userValidator;

	@Mock
	private StockReservationComponent stockReservationComponent;

	@Mock
	private PaymentProcessorComposite paymentProcessorComposite;

	@Mock
	private PaymentValidator paymentValidator;

	@Mock
	private IdempotencyService idempotencyService;

	private BookingFacade bookingFacade;

	@BeforeEach
	void setUp() {
		bookingFacade = new BookingFacade(
				bookingService,
				eventProductRetriever,
				userValidator,
				stockReservationComponent,
				paymentProcessorComposite,
				paymentValidator,
				idempotencyService
		);
	}

	@Test
	@DisplayName("재고 선점과 결제가 성공하면 예약을 확정한다")
	void createBooking_성공_예약확정() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.SUCCESS));
		given(bookingService.getBookingResponse(BOOKING_ID)).willReturn(bookingResponse());

		// when
		BookingCreateResult response = bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest());

		// then
		assertThat(response.body()).isEqualTo(bookingResponse());
		then(bookingService).should().confirmBooking(BOOKING_ID);
	}

	@Test
	@DisplayName("예약 생성 전 결제 요청을 검증한다")
	void createBooking_성공_결제요청검증() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.SUCCESS));
		given(bookingService.getBookingResponse(BOOKING_ID)).willReturn(bookingResponse());

		// when
		bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest());

		// then
		then(paymentValidator).should().validate(BigDecimal.valueOf(100_000), cardRequest().payments());
	}

	@Test
	@DisplayName("재고 선점 후 재고 이력과 대기 결제를 생성한다")
	void createBooking_성공_재고이력과대기결제생성() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.SUCCESS));
		given(bookingService.getBookingResponse(BOOKING_ID)).willReturn(bookingResponse());

		// when
		bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest());

		// then
		then(bookingService).should().createReserveHistoryAndPendingPayments(
				booking,
				eventProduct(),
				cardRequest().payments(),
				StockReservationMethod.REDIS
		);
	}

	@Test
	@DisplayName("포인트가 아닌 결제 수단은 외부 결제를 승인한다")
	void createBooking_성공_외부결제승인() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.SUCCESS));
		given(bookingService.getBookingResponse(BOOKING_ID)).willReturn(bookingResponse());

		// when
		bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest());

		// then
		then(paymentProcessorComposite).should()
				.approve(PaymentMethod.CREDIT_CARD, "booking-100-CREDIT_CARD", BigDecimal.valueOf(100_000));
	}

	@Test
	@DisplayName("Redis 재고 선점 결과 품절이면 예약을 취소하고 품절 예외를 반환한다")
	void createBooking_품절_예약취소() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.SOLD_OUT));

		// when & then
		assertThatThrownBy(() -> bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest()))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOLD_OUT));
		then(bookingService).should().cancelBooking(BOOKING_ID);
		then(bookingService).should(never()).createReserveHistoryAndPendingPayments(any(), any(), any(), any());
		then(paymentProcessorComposite).should(never())
				.approve(any(PaymentMethod.class), any(String.class), any(BigDecimal.class));
	}

	@Test
	@DisplayName("같은 사용자가 같은 상품을 다시 예약하면 예약을 취소하고 중복 예약 예외를 반환한다")
	void createBooking_이미예약함_예약취소() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.ALREADY_RESERVED));

		// when & then
		assertThatThrownBy(() -> bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest()))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_RESERVED));
		then(bookingService).should().cancelBooking(BOOKING_ID);
		then(paymentProcessorComposite).should(never())
				.approve(any(PaymentMethod.class), any(String.class), any(BigDecimal.class));
	}

	@Test
	@DisplayName("외부 결제가 실패하면 포인트와 재고를 보상하고 예약을 결제 실패로 전환한다")
	void createBooking_외부결제실패_보상처리() {
		// given
		BookingEntity booking = bookingEntity();
		givenDefaultCreationStubs(booking);
		given(stockReservationComponent.reserve(EVENT_PRODUCT_ID, 10, BOOKING_ID, USER_ID))
				.willReturn(StockReservation.redis(StockReservationResult.SUCCESS));
		willThrow(new RuntimeException("pg failed")).given(paymentProcessorComposite)
				.approve(PaymentMethod.CREDIT_CARD, "booking-100-CREDIT_CARD", BigDecimal.valueOf(90_000));

		// when & then
		assertThatThrownBy(() -> bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardAndPointRequest()))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED));
		then(bookingService).should().compensatePaymentFailure(booking, eventProduct(), cardAndPointRequest().payments());
		then(stockReservationComponent).should().release(EVENT_PRODUCT_ID, BOOKING_ID, USER_ID, StockReservationMethod.REDIS);
	}

	@Test
	@DisplayName("같은 멱등키의 완료 응답이 있으면 저장된 응답을 반환한다")
	void createBooking_동일멱등키_저장응답반환() {
		// given
		given(idempotencyService.checkOrCreateIdempotency(IDEMPOTENCY_KEY, CARD_REQUEST_BODY))
				.willReturn(IdempotencyCurrentPosition.replayStoredResponse(HttpStatus.CREATED.value(), bookingResponse()));

		// when
		BookingCreateResult response = bookingFacade.createBooking(USER_ID, IDEMPOTENCY_KEY, cardRequest());

		// then
		assertThat(response.body()).isEqualTo(bookingResponse());
		then(eventProductRetriever).should(never()).getEventProductFromRedis(anyLong());
		then(stockReservationComponent).should(never()).reserve(anyLong(), anyInt(), anyLong(), anyLong());
	}

	private void givenDefaultCreationStubs(final BookingEntity booking) {
		given(idempotencyService.checkOrCreateIdempotency(eq(IDEMPOTENCY_KEY), anyString()))
				.willReturn(IdempotencyCurrentPosition.processNewRequest());
		given(eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID)).willReturn(eventProduct());
		given(bookingService.createPendingBooking(USER_ID, eventProduct())).willReturn(booking);
	}

	private EventProduct eventProduct() {
		return new EventProduct(
				EVENT_PRODUCT_ID,
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				10,
				LocalDateTime.of(2026, 6, 1, 15, 0),
				LocalDateTime.of(2026, 6, 2, 11, 0),
				LocalDateTime.of(2020, 1, 1, 0, 0)
		);
	}

	private BookingEntity bookingEntity() {
		BookingEntity booking = BookingEntity.create(
				EVENT_PRODUCT_ID,
				USER_ID,
				BigDecimal.valueOf(100_000),
				LocalDateTime.now().plusMinutes(3),
				StockReservationMethod.REDIS
		);
		ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
		return booking;
	}

	private BookingCreateRequest cardRequest() {
		return new BookingCreateRequest(EVENT_PRODUCT_ID, List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(100_000))
		));
	}

	private BookingCreateRequest cardAndPointRequest() {
		return new BookingCreateRequest(EVENT_PRODUCT_ID, List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(90_000)),
				new BookingPaymentRequest(PaymentMethod.Y_POINT, BigDecimal.valueOf(10_000))
		));
	}

	private BookingResponse bookingResponse() {
		return new BookingResponse(
				BOOKING_ID,
				EVENT_PRODUCT_ID,
				USER_ID,
				BookingStatus.CONFIRMED,
				BigDecimal.valueOf(100_000),
				LocalDateTime.of(2026, 5, 7, 10, 3),
				List.of(new BookingPaymentResponse(
						1L,
						PaymentMethod.CREDIT_CARD,
						BigDecimal.valueOf(100_000),
						PaymentStatus.COMPLETED
				))
		);
	}
}
