package booking.server.domain.recovery;

import booking.server.domain.stock.component.StockHistoryRetriever;
import booking.server.global.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisStockRebuildService {

	private static final String STOCK_USED_KEY_FORMAT = "event-product:%d:stock:used";
	private static final String STOCK_USER_KEY_FORMAT = "event-product:%d:stock:users";

	private final StockHistoryRetriever stockHistoryRetriever;
	private final RedisClient redisClient;

	public void rebuildAll() {
		for (Long eventProductId : stockHistoryRetriever.getEventProductIds()) {
			redisClient.replaceSetMembers(
					STOCK_USED_KEY_FORMAT.formatted(eventProductId),
					stockHistoryRetriever.getActiveBookingIds(eventProductId)
			);
			redisClient.replaceSetMembers(
					STOCK_USER_KEY_FORMAT.formatted(eventProductId),
					stockHistoryRetriever.getActiveUserIds(eventProductId)
			);
		}
	}
}
