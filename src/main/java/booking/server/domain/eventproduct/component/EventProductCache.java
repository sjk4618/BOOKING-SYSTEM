package booking.server.domain.eventproduct.component;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.global.redis.RedisClient;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class EventProductCache {

	private static final String KEY_PREFIX = "checkout:event-product:";
	private static final int BASE_TTL_SECONDS = 600;
	private static final int JITTER_BOUND_SECONDS = 60;

	private final RedisClient redisClient;

	public EventProductCache(final RedisClient redisClient) {
		this.redisClient = redisClient;
	}

	public Optional<EventProduct> getFromRedis(final Long eventProductId) {
		return redisClient.getJson(key(eventProductId), EventProduct.class);
	}

	public void put(final EventProduct snapshot) {
		redisClient.setJson(key(snapshot.eventProductId()), snapshot, ttl());
	}

	private String key(final Long eventProductId) {
		return KEY_PREFIX + eventProductId;
	}

	private Duration ttl() {
		int jitter = ThreadLocalRandom.current().nextInt(JITTER_BOUND_SECONDS + 1);
		return Duration.ofSeconds(BASE_TTL_SECONDS + jitter);
	}
}
