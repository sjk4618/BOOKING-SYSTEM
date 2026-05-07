package booking.server.domain.eventproduct.component;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import booking.server.domain.eventproduct.repository.EventProductRepository;
import booking.server.global.redis.RedisClient;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventProductCacheWarmer {

	private static final String WARM_UP_LOCK_KEY = "lock:checkout:event-product:warm-up";
	private static final Duration WARM_UP_LOCK_TTL = Duration.ofSeconds(30);

	private final EventProductRepository eventProductRepository;
	private final EventProductCache eventProductCache;
	private final RedisClient redisClient;

	@EventListener(ApplicationReadyEvent.class)
	public void warmUp() {
		if (!redisClient.tryLock(WARM_UP_LOCK_KEY, WARM_UP_LOCK_TTL)) {
			return;
		}

		try {
			List<EventProductEntity> eventProducts = eventProductRepository.findAll();
			for (EventProductEntity eventProduct : eventProducts) {
				if (eventProduct.getId() != null) {
					eventProductCache.put(EventProduct.fromEntity(eventProduct.getId(), eventProduct));
				}
			}
		} finally {
			redisClient.unlock(WARM_UP_LOCK_KEY);
		}
	}
}
