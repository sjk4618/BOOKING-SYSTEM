package booking.server.domain.stock.component;

import booking.server.domain.stock.domain.entity.StockReservationMethod;

public record StockReservation(
		StockReservationResult result,
		StockReservationMethod method
) {

	public static StockReservation redis(final StockReservationResult result) {
		return new StockReservation(result, StockReservationMethod.REDIS);
	}

	public static StockReservation database(final StockReservationResult result) {
		return new StockReservation(result, StockReservationMethod.DATABASE);
	}
}
