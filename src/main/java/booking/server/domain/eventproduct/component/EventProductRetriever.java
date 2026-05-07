package booking.server.domain.eventproduct.component;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import booking.server.domain.eventproduct.exception.EventProductNotFoundException;
import booking.server.domain.eventproduct.repository.EventProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventProductRetriever {

	private final EventProductRepository eventProductRepository;
	private final EventProductCache eventProductCache;

	public EventProduct getEventProductFromRedis(final Long eventProductId) {
		return eventProductCache.getFromRedis(eventProductId)
				.orElseGet(() -> getFromDatabase(eventProductId));
	}

	private EventProduct getFromDatabase(final Long eventProductId) {
		EventProductEntity eventProduct = eventProductRepository.findById(eventProductId)
				.orElseThrow(EventProductNotFoundException::new);
		EventProduct snapshot = EventProduct.fromEntity(eventProductId, eventProduct);
		eventProductCache.put(snapshot);
		return snapshot;
	}
}
