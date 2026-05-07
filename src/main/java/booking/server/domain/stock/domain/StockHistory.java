package booking.server.domain.stock.domain;

import booking.server.domain.stock.domain.entity.StockHistoryEntity;
import booking.server.domain.stock.domain.entity.StockHistoryType;
import booking.server.domain.stock.domain.entity.StockReservationMethod;
import java.math.BigDecimal;

public record StockHistory(
		long stockHistoryId,
		long eventProductId,
		long bookingId,
		long userId,
		BigDecimal price,
		StockHistoryType type,
		StockReservationMethod reservationMethod
) {

	public static StockHistory fromEntity(final StockHistoryEntity stockHistory) {
		return new StockHistory(
				stockHistory.getId(),
				stockHistory.getEventProductId(),
				stockHistory.getBookingId(),
				stockHistory.getUserId(),
				stockHistory.getPrice(),
				stockHistory.getType(),
				stockHistory.getReservationMethod()
		);
	}
}
