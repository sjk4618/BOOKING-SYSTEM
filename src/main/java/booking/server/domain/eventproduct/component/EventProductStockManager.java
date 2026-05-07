package booking.server.domain.eventproduct.component;

import booking.server.domain.eventproduct.repository.EventProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventProductStockManager {

	private final EventProductRepository eventProductRepository;

	public boolean increaseIfAvailable(final long eventProductId) {
		return eventProductRepository.increaseUsedStockIfAvailable(eventProductId) > 0;
	}

	public void decrease(final long eventProductId) {
		eventProductRepository.decreaseUsedStock(eventProductId);
	}
}
