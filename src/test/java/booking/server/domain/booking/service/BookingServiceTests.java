package booking.server.domain.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import booking.server.domain.booking.component.BookingRetriever;
import booking.server.domain.booking.component.BookingSaver;
import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.payment.component.PaymentRetriever;
import booking.server.domain.payment.component.PaymentSaver;
import booking.server.domain.payment.component.PaymentValidator;
import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import booking.server.domain.stock.component.StockHistoryRetriever;
import booking.server.domain.stock.component.StockHistorySaver;
import booking.server.domain.stock.domain.entity.StockHistoryEntity;
import booking.server.domain.stock.domain.entity.StockReservationMethod;
import booking.server.domain.user.component.UserPointManager;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookingServiceTests {

	private static final long USER_ID = 10L;
	private static final long EVENT_PRODUCT_ID = 1L;
	private static final long BOOKING_ID = 100L;

	@Mock
	private BookingRetriever bookingRetriever;

	@Mock
	private BookingSaver bookingSaver;

	@Mock
	private PaymentRetriever paymentRetriever;

	@Mock
	private PaymentSaver paymentSaver;

	@Mock
	private PaymentValidator paymentValidator;

	@Mock
	private StockHistorySaver stockHistorySaver;

	@Mock
	private StockHistoryRetriever stockHistoryRetriever;

	@Mock
	private UserPointManager userPointManager;

	private BookingService bookingService;

	@BeforeEach
	void setUp() {
		bookingService = new BookingService(
				bookingRetriever,
				bookingSaver,
				paymentRetriever,
				paymentSaver,
				paymentValidator,
				stockHistorySaver,
				stockHistoryRetriever,
				userPointManager
		);
	}

	@Test
	@DisplayName("대기 예약을 생성한다")
	void createPendingBooking_성공_대기예약생성() {
		// given
		given(bookingSaver.save(any(BookingEntity.class))).willAnswer(invocation -> {
			BookingEntity booking = invocation.getArgument(0);
			ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
			return booking;
		});

		// when
		BookingEntity booking = bookingService.createPendingBooking(USER_ID, eventProduct());

		// then
		assertThat(booking)
				.extracting(BookingEntity::getId, BookingEntity::getStatus, BookingEntity::getStockReservationMethod)
				.containsExactly(BOOKING_ID, BookingStatus.PENDING, StockReservationMethod.REDIS);
	}

	@Test
	@DisplayName("재고 이력과 대기 결제를 저장하고 포인트를 차감한다")
	void createReserveHistoryAndPendingPayments_포인트포함_저장과차감() {
		// given
		BookingEntity booking = bookingEntity();
		given(stockHistoryRetriever.hasActiveReservation(EVENT_PRODUCT_ID, USER_ID)).willReturn(false);
		given(paymentValidator.pointAmount(cardAndPointPayments())).willReturn(10_000);
		given(userPointManager.deduct(USER_ID, 10_000)).willReturn(true);
		given(paymentSaver.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

		// when
		bookingService.createReserveHistoryAndPendingPayments(
				booking,
				eventProduct(),
				cardAndPointPayments(),
				StockReservationMethod.REDIS
		);

		// then
		then(stockHistorySaver).should().save(any(StockHistoryEntity.class));
		then(paymentSaver).should().saveAll(any());
		then(userPointManager).should().deduct(USER_ID, 10_000);
	}

	@Test
	@DisplayName("이미 같은 상품을 예약한 사용자는 대기 결제를 생성하지 않는다")
	void createReserveHistoryAndPendingPayments_이미예약함_예외() {
		// given
		BookingEntity booking = bookingEntity();
		given(stockHistoryRetriever.hasActiveReservation(EVENT_PRODUCT_ID, USER_ID)).willReturn(true);

		// when & then
		assertThatThrownBy(() -> bookingService.createReserveHistoryAndPendingPayments(
				booking,
				eventProduct(),
				cardAndPointPayments(),
				StockReservationMethod.REDIS
		)).isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_RESERVED));
		then(paymentSaver).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("포인트가 부족하면 결제 대기 생성 트랜잭션을 실패시킨다")
	void createReserveHistoryAndPendingPayments_포인트부족_예외() {
		// given
		BookingEntity booking = bookingEntity();
		given(stockHistoryRetriever.hasActiveReservation(EVENT_PRODUCT_ID, USER_ID)).willReturn(false);
		given(paymentValidator.pointAmount(cardAndPointPayments())).willReturn(10_000);
		given(userPointManager.deduct(USER_ID, 10_000)).willReturn(false);

		// when & then
		assertThatThrownBy(() -> bookingService.createReserveHistoryAndPendingPayments(
				booking,
				eventProduct(),
				cardAndPointPayments(),
				StockReservationMethod.REDIS
		)).isInstanceOfSatisfying(BusinessException.class,
				exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.POINT_NOT_ENOUGH));
	}

	@Test
	@DisplayName("결제를 완료하고 예약을 확정한다")
	void confirmBooking_성공_결제완료예약확정() {
		// given
		BookingEntity booking = bookingEntity();
		List<PaymentEntity> payments = new ArrayList<>(List.of(paymentEntity()));
		given(paymentRetriever.getByBookingId(BOOKING_ID)).willReturn(payments);
		given(bookingRetriever.get(BOOKING_ID)).willReturn(booking);

		// when
		bookingService.confirmBooking(BOOKING_ID);

		// then
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
		assertThat(payments).allSatisfy(payment -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED));
	}

	@Test
	@DisplayName("결제 실패 보상 시 포인트를 복구하고 결제와 예약을 실패 상태로 전환한다")
	void compensatePaymentFailure_성공_포인트복구와상태전환() {
		// given
		BookingEntity booking = bookingEntity();
		List<PaymentEntity> payments = new ArrayList<>(List.of(paymentEntity()));
		given(paymentValidator.pointAmount(cardAndPointPayments())).willReturn(10_000);
		given(paymentRetriever.getByBookingId(BOOKING_ID)).willReturn(payments);
		given(bookingRetriever.get(BOOKING_ID)).willReturn(booking);

		// when
		bookingService.compensatePaymentFailure(booking, eventProduct(), cardAndPointPayments());

		// then
		then(userPointManager).should().restore(USER_ID, 10_000);
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
		assertThat(payments).allSatisfy(payment -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED));
		then(stockHistorySaver).should().save(any(StockHistoryEntity.class));
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

	private PaymentEntity paymentEntity() {
		PaymentEntity payment = PaymentEntity.create(
				BOOKING_ID,
				PaymentMethod.CREDIT_CARD,
				BigDecimal.valueOf(100_000),
				"booking-100-CREDIT_CARD"
		);
		ReflectionTestUtils.setField(payment, "id", 1L);
		return payment;
	}

	private List<BookingPaymentRequest> cardAndPointPayments() {
		return List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(90_000)),
				new BookingPaymentRequest(PaymentMethod.Y_POINT, BigDecimal.valueOf(10_000))
		);
	}
}
