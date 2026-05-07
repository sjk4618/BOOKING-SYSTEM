package booking.server.domain.stock.component;

import booking.server.domain.eventproduct.component.EventProductStockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DatabaseStockReservationComponent {

	private final EventProductStockManager eventProductStockManager;
	private final StockHistoryRetriever stockHistoryRetriever;

	@Transactional
	public StockReservationResult reserve(final long eventProductId, final long userId) {
		if (stockHistoryRetriever.hasActiveReservation(eventProductId, userId)) {
			return StockReservationResult.ALREADY_RESERVED;
		}
		if (eventProductStockManager.increaseIfAvailable(eventProductId)) {
			return StockReservationResult.SUCCESS;
		}
		return StockReservationResult.SOLD_OUT;
	}

	@Transactional
	public void release(final long eventProductId) {
		eventProductStockManager.decrease(eventProductId);
	}
}
