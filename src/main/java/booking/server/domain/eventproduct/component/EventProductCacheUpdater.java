package booking.server.domain.eventproduct.component;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class EventProductCacheUpdater {

	private final EventProductCache eventProductCache;

	public void updateAfterCommit(final EventProductEntity eventProduct) {
		if (eventProduct.getId() == null) {
			return;
		}

		EventProduct snapshot = EventProduct.fromEntity(eventProduct.getId(), eventProduct);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			eventProductCache.put(snapshot);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				eventProductCache.put(snapshot);
			}
		});
	}
}
