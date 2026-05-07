package booking.server.domain.booking.domain;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Booking(
		long bookingId,
		long eventProductId,
		Long userId,
		BookingStatus status,
		BigDecimal totalAmount,
		LocalDateTime reservedUntil
) {

	public static Booking fromEntity(final BookingEntity booking) {
		return new Booking(
				booking.getId(),
				booking.getEventProductEntityId(),
				booking.getUserId(),
				booking.getStatus(),
				booking.getTotalAmount(),
				booking.getReservedUntil()
		);
	}
}
