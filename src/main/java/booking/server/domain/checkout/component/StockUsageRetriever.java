package booking.server.domain.checkout.component;

import booking.server.global.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockUsageRetriever {

	private static final String STOCK_USED_KEY_FORMAT = "event-product:%d:stock:used";

	private final RedisClient redisClient;

	public StockUsageResult getStockUsage(final long eventProductId) {
		return redisClient.getSetSize(key(eventProductId))
				.map(StockUsageResult::success)
				.orElseGet(StockUsageResult::unavailable);
	}

	private String key(final Long eventProductId) {
		return STOCK_USED_KEY_FORMAT.formatted(eventProductId);
	}
}
