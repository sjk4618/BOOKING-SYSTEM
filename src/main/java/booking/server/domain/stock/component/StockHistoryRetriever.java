package booking.server.domain.stock.component;

import booking.server.domain.stock.repository.StockHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockHistoryRetriever {

	private final StockHistoryRepository stockHistoryRepository;

	public List<Long> getEventProductIds() {
		return stockHistoryRepository.findDistinctEventProductIds();
	}

	public List<Long> getActiveBookingIds(final long eventProductId) {
		return stockHistoryRepository.findActiveBookingIdsByEventProductId(eventProductId);
	}

	public List<Long> getActiveUserIds(final long eventProductId) {
		return stockHistoryRepository.findActiveUserIdsByEventProductId(eventProductId);
	}

	public boolean hasActiveReservation(final long eventProductId, final long userId) {
		return stockHistoryRepository.existsActiveReservationByEventProductIdAndUserId(eventProductId, userId);
	}
}
