package booking.server.domain.recovery;

import booking.server.domain.stock.domain.entity.StockReservationMethod;

public record ExpiredBookingRelease(
		long eventProductId,
		long bookingId,
		long userId,
		StockReservationMethod reservationMethod
) {
}
