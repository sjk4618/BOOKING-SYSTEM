package booking.server.domain.booking.dto;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import booking.server.domain.payment.domain.entity.PaymentEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
		long bookingId,
		long eventProductId,
		long userId,
		BookingStatus status,
		BigDecimal totalAmount,
		LocalDateTime reservedUntil,
		List<BookingPaymentResponse> payments
) {

	public static BookingResponse from(final BookingEntity booking, final List<PaymentEntity> payments) {
		return new BookingResponse(
				booking.getId(),
				booking.getEventProductEntityId(),
				booking.getUserId(),
				booking.getStatus(),
				booking.getTotalAmount(),
				booking.getReservedUntil(),
				payments.stream()
						.map(BookingPaymentResponse::from)
						.toList()
		);
	}
}
