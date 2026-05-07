package booking.server.domain.booking.service;

import booking.server.domain.booking.component.BookingRetriever;
import booking.server.domain.booking.component.BookingSaver;
import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.booking.dto.BookingResponse;
import booking.server.domain.booking.exception.BookingNotFoundException;
import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.payment.component.PaymentRetriever;
import booking.server.domain.payment.component.PaymentSaver;
import booking.server.domain.payment.component.PaymentValidator;
import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.exception.InvalidPaymentAmountException;
import booking.server.domain.stock.component.StockHistorySaver;
import booking.server.domain.stock.component.StockHistoryRetriever;
import booking.server.domain.stock.domain.entity.StockHistoryEntity;
import booking.server.domain.stock.domain.entity.StockReservationMethod;
import booking.server.domain.user.component.UserPointManager;
import booking.server.global.exception.BadRequestException;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingService {

	private static final int RESERVATION_MINUTES = 3;

	private final BookingRetriever bookingRetriever;
	private final BookingSaver bookingSaver;
	private final PaymentRetriever paymentRetriever;
	private final PaymentSaver paymentSaver;
	private final PaymentValidator paymentValidator;
	private final StockHistorySaver stockHistorySaver;
	private final StockHistoryRetriever stockHistoryRetriever;
	private final UserPointManager userPointManager;

	@Transactional
	public BookingEntity createPendingBooking(final long userId,
											  final EventProduct eventProduct) {
		return bookingSaver.save(BookingEntity.create(
				eventProduct.eventProductId(),
				userId,
				eventProduct.price(),
				LocalDateTime.now().plusMinutes(RESERVATION_MINUTES),
				StockReservationMethod.REDIS
		));
	}

	@Transactional
	public void createReserveHistoryAndPendingPayments(final BookingEntity booking,
													   final EventProduct eventProduct,
													   final List<BookingPaymentRequest> paymentRequests,
													   final StockReservationMethod reservationMethod) {
		if (stockHistoryRetriever.hasActiveReservation(eventProduct.eventProductId(), booking.getUserId())) {
			throw new BusinessException(ErrorCode.ALREADY_RESERVED);
		}
		stockHistorySaver.save(StockHistoryEntity.reserve(
				eventProduct.eventProductId(),
				booking.getId(),
				booking.getUserId(),
				eventProduct.price(),
				reservationMethod
		));
		paymentSaver.saveAll(paymentRequests.stream()
				.map(payment -> PaymentEntity.create(
						booking.getId(),
						payment.paymentMethod(),
						payment.amount(),
						requestKey(booking.getId(), payment.paymentMethod())
				))
				.toList());
		int pointAmount = pointAmount(paymentRequests);
		if (pointAmount > 0 && !userPointManager.deduct(booking.getUserId(), pointAmount)) {
			throw new BusinessException(ErrorCode.POINT_NOT_ENOUGH);
		}
	}

	@Transactional
	public void confirmBooking(final long bookingId) {
		List<PaymentEntity> payments = paymentRetriever.getByBookingId(bookingId);
		payments.forEach(PaymentEntity::complete);
		BookingEntity booking = getBooking(bookingId);
		booking.confirm();
	}

	@Transactional
	public void compensatePaymentFailure(final BookingEntity booking,
										 final EventProduct eventProduct,
										 final List<BookingPaymentRequest> paymentRequests) {
		int pointAmount = pointAmount(paymentRequests);
		if (pointAmount > 0) {
			userPointManager.restore(booking.getUserId(), pointAmount);
		}
		paymentRetriever.getByBookingId(booking.getId()).forEach(PaymentEntity::fail);
		getBooking(booking.getId()).failPayment();
		stockHistorySaver.save(StockHistoryEntity.release(
				eventProduct.eventProductId(),
				booking.getId(),
				booking.getUserId(),
				eventProduct.price(),
				booking.getStockReservationMethod()
		));
	}

	@Transactional
	public void changeStockReservationMethod(final long bookingId, final StockReservationMethod reservationMethod) {
		getBooking(bookingId).changeStockReservationMethod(reservationMethod);
	}

	@Transactional
	public void markPaymentUnknown(final long bookingId) {
		getBooking(bookingId).markPaymentUnknown();
	}

	@Transactional
	public void failBooking(final long bookingId) {
		getBooking(bookingId).failPayment();
	}

	@Transactional
	public void cancelBooking(final long bookingId) {
		getBooking(bookingId).cancel();
	}

	@Transactional(readOnly = true)
	public BookingResponse getBookingResponse(final long bookingId) {
		BookingEntity booking = getBooking(bookingId);
		return BookingResponse.from(booking, paymentRetriever.getByBookingId(bookingId));
	}

	@Transactional(readOnly = true)
	public BookingResponse getIdempotentResponse(final BookingEntity booking,
												 final long userId,
												 final long eventProductId) {
		if (!booking.getUserId().equals(userId) || booking.getEventProductEntityId() != eventProductId) {
			throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
		}
		return BookingResponse.from(booking, paymentRetriever.getByBookingId(booking.getId()));
	}

	private BookingEntity getBooking(final long bookingId) {
		try {
			return bookingRetriever.get(bookingId);
		} catch (BookingNotFoundException exception) {
			throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
		}
	}

	private int pointAmount(final List<BookingPaymentRequest> paymentRequests) {
		try {
			return paymentValidator.pointAmount(paymentRequests);
		} catch (InvalidPaymentAmountException exception) {
			throw new BadRequestException(ErrorCode.BAD_REQUEST);
		}
	}

	private String requestKey(final long bookingId, final PaymentMethod paymentMethod) {
		return "booking-%d-%s".formatted(bookingId, paymentMethod.name());
	}
}
