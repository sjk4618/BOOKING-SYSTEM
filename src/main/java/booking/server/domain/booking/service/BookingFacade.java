package booking.server.domain.booking.service;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.dto.BookingCreateResult;
import booking.server.domain.booking.dto.BookingCreateRequest;
import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.booking.dto.BookingResponse;
import booking.server.domain.eventproduct.component.EventProductRetriever;
import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.exception.EventProductNotFoundException;
import booking.server.domain.idempotency.service.IdempotencyCurrentPosition;
import booking.server.domain.idempotency.service.IdempotencyService;
import booking.server.domain.payment.component.PaymentProcessorComposite;
import booking.server.domain.payment.component.PaymentValidator;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.exception.InvalidPaymentAmountException;
import booking.server.domain.payment.exception.InvalidPaymentMethodException;
import booking.server.domain.stock.component.StockReservation;
import booking.server.domain.stock.component.StockReservationComponent;
import booking.server.domain.stock.component.StockReservationResult;
import booking.server.domain.stock.domain.entity.StockReservationMethod;
import booking.server.domain.user.component.UserValidator;
import booking.server.domain.user.exception.UserNotFoundException;
import booking.server.global.exception.BadRequestException;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import booking.server.global.exception.ErrorResponse;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingFacade {

	private final BookingService bookingService;
	private final EventProductRetriever eventProductRetriever;
	private final UserValidator userValidator;
	private final StockReservationComponent stockReservationComponent;
	private final PaymentProcessorComposite paymentProcessorComposite;
	private final PaymentValidator paymentValidator;
	private final IdempotencyService idempotencyService;

	public BookingCreateResult createBooking(final long userId,
											 final String idempotencyKey,
											 final BookingCreateRequest request) {
		validateIdempotencyKey(idempotencyKey);
		// 동일 멱등키 재시도면 DB에 저장된 응답을 그대로 재생한다.
		final IdempotencyCurrentPosition idempotency = idempotencyService.checkOrCreateIdempotency(idempotencyKey, requestBody(userId, request));
		if (idempotency.hasStoredResponse()) {
			return new BookingCreateResult(idempotency.httpStatus(), idempotency.responseBody());
		}

		try {
			final EventProduct eventProduct = getEventProduct(request.eventProductId());
			validateUser(userId);
			validatePayments(eventProduct.price(), request.payments());

			final BookingEntity booking = bookingService.createPendingBooking(userId, eventProduct);
			StockReservationMethod reservationMethod = reserveStockOrCancel(booking, eventProduct);

			try {
				bookingService.createReserveHistoryAndPendingPayments(
						booking,
						eventProduct,
						request.payments(),
						reservationMethod
				);
			} catch (BusinessException exception) {
				// 내부 저장 실패 시 선점 재고를 즉시 반환한다.
				releaseStockAndFailBooking(booking, eventProduct);
				throw exception;
			} catch (RuntimeException exception) {
				releaseStockAndFailBooking(booking, eventProduct);
				throw new BusinessException(ErrorCode.PAYMENT_FAILED);
			}

			try {
				approveExternalPayments(booking, request.payments());
			} catch (RuntimeException exception) {
				compensatePaymentFailure(booking, eventProduct, request.payments());
				throw new BusinessException(ErrorCode.PAYMENT_FAILED);
			}

			confirmBookingOrMarkUnknown(booking, eventProduct, request.payments());
			BookingResponse response = bookingService.getBookingResponse(booking.getId());
			idempotencyService.succeed(idempotencyKey, booking.getId(), HttpStatus.CREATED.value(), response);
			return new BookingCreateResult(HttpStatus.CREATED.value(), response);
		} catch (BusinessException exception) {
			recordFailure(idempotencyKey, exception.getErrorCode());
			throw exception;
		} catch (BadRequestException exception) {
			recordFailure(idempotencyKey, exception.getErrorCode());
			throw exception;
		}
	}

	private EventProduct getEventProduct(final long eventProductId) {
		try {
			return eventProductRetriever.getEventProductFromRedis(eventProductId);
		} catch (EventProductNotFoundException exception) {
			throw new BusinessException(ErrorCode.EVENT_PRODUCT_NOT_FOUND);
		}
	}

	private void validateUser(final long userId) {
		try {
			userValidator.validateExists(userId);
		} catch (UserNotFoundException exception) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
	}

	private void validatePayments(final BigDecimal eventProductPrice, final List<BookingPaymentRequest> payments) {
		try {
			paymentValidator.validate(eventProductPrice, payments);
		} catch (InvalidPaymentMethodException exception) { //중복 결제수단, 신용카드와 Y페이 동시 사용 등
			throw new BusinessException(ErrorCode.INVALID_PAYMENT_METHOD);
		} catch (InvalidPaymentAmountException exception) { //상품 가격과 결제금액 합계 불일치, 0 이하 금액, 포인트 소수점 등
			throw new BadRequestException(ErrorCode.BAD_REQUEST);
		}
	}

	private StockReservationMethod reserveStockOrCancel(final BookingEntity booking, final EventProduct eventProduct) {
		StockReservation reservation = stockReservationComponent.reserve(
				eventProduct.eventProductId(),
				eventProduct.totalStock(),
				booking.getId(),
				booking.getUserId()
		);
		StockReservationResult result = reservation.result();

		if (result == StockReservationResult.SUCCESS || result == StockReservationResult.DUPLICATED) {
			// Redis 장애로 DB 선점으로 전환된 경우, 이후 보상 해제를 위해 예약 방식을 booking에 저장한다.
			if (reservation.method() == StockReservationMethod.DATABASE) {
				booking.changeStockReservationMethod(StockReservationMethod.DATABASE);
				bookingService.changeStockReservationMethod(booking.getId(), StockReservationMethod.DATABASE);
			}
			return reservation.method();
		}
		bookingService.cancelBooking(booking.getId());
		if (result == StockReservationResult.SOLD_OUT) {
			throw new BusinessException(ErrorCode.SOLD_OUT);
		}
		if (result == StockReservationResult.ALREADY_RESERVED) {
			throw new BusinessException(ErrorCode.ALREADY_RESERVED);
		}
		throw new BusinessException(ErrorCode.STOCK_RESERVATION_FAILED);
	}

	private void approveExternalPayments(final BookingEntity booking, final List<BookingPaymentRequest> payments) {
		for (BookingPaymentRequest payment : payments) {
			paymentProcessorComposite.approve(
					payment.paymentMethod(),
					requestKey(booking.getId(), payment.paymentMethod()),
					payment.amount()
			);
		}
	}

	private void confirmBookingOrMarkUnknown(final BookingEntity booking,
											 final EventProduct eventProduct,
											 final List<BookingPaymentRequest> paymentRequests) {
		try {
			bookingService.confirmBooking(booking.getId());
		} catch (RuntimeException exception) {
			try {
				compensateExternalPayments(booking, paymentRequests);
				bookingService.compensatePaymentFailure(booking, eventProduct, paymentRequests);
					stockReservationComponent.release(
							eventProduct.eventProductId(),
							booking.getId(),
							booking.getUserId(),
							booking.getStockReservationMethod()
					);
			} catch (RuntimeException compensationException) {
				bookingService.markPaymentUnknown(booking.getId());
			}
			throw new BusinessException(ErrorCode.PAYMENT_FAILED);
		}
	}

	private void compensateExternalPayments(final BookingEntity booking, final List<BookingPaymentRequest> payments) {
		for (BookingPaymentRequest payment : payments) {
			paymentProcessorComposite.compensate(
					payment.paymentMethod(),
					requestKey(booking.getId(), payment.paymentMethod()),
					payment.amount()
			);
		}
	}

	private void compensatePaymentFailure(final BookingEntity booking,
										  final EventProduct eventProduct,
										  final List<BookingPaymentRequest> paymentRequests) {
		bookingService.compensatePaymentFailure(booking, eventProduct, paymentRequests);
		stockReservationComponent.release(
				eventProduct.eventProductId(),
				booking.getId(),
				booking.getUserId(),
				booking.getStockReservationMethod()
		);
	}

	private void releaseStockAndFailBooking(final BookingEntity booking, final EventProduct eventProduct) {
		bookingService.failBooking(booking.getId());
		stockReservationComponent.release(
				eventProduct.eventProductId(),
				booking.getId(),
				booking.getUserId(),
				booking.getStockReservationMethod()
		);
	}

	private String requestKey(final long bookingId, final PaymentMethod paymentMethod) {
		return "booking-%d-%s".formatted(bookingId, paymentMethod.name());
	}

	private void validateIdempotencyKey(final String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new BadRequestException(ErrorCode.BAD_REQUEST);
		}
	}

	private String requestBody(final long userId, final BookingCreateRequest request) {
		String payments = String.join(
				",",
				request.payments().stream()
						.sorted(Comparator.comparing(payment -> payment.paymentMethod().name()))
						.map(payment -> "%s=%s".formatted(payment.paymentMethod().name(), normalized(payment.amount())))
						.toList()
		);
		return "userId=%d|eventProductId=%d|payments=[%s]".formatted(userId, request.eventProductId(), payments);
	}

	private String normalized(final BigDecimal amount) {
		return amount.stripTrailingZeros().toPlainString();
	}

	private void recordFailure(final String idempotencyKey, final ErrorCode errorCode) {
		ErrorResponse response = ErrorResponse.from(errorCode);
		idempotencyService.fail(idempotencyKey, errorCode.getStatus().value(), response);
	}
}
