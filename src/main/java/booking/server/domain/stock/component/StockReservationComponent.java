package booking.server.domain.stock.component;

import booking.server.domain.stock.domain.entity.StockReservationMethod;
import booking.server.global.redis.RedisClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockReservationComponent {

	private static final String STOCK_USED_KEY_FORMAT = "event-product:%d:stock:used";
	private static final String STOCK_USER_KEY_FORMAT = "event-product:%d:stock:users";
	private static final String STOCK_SOLD_OUT_KEY_FORMAT = "event-product:%d:stock:sold-out";
	private static final long SOLD_OUT_MARKER_TTL_SECONDS = 30L;
	private static final long RESERVE_SUCCESS = 1L;
	private static final long RESERVE_SOLD_OUT = 0L;
	private static final long RESERVE_ALREADY_RESERVED = 2L;
	private static final long RESERVE_DUPLICATED = 3L;

	private final RedisClient redisClient;
	private final DatabaseStockReservationComponent databaseStockReservationComponent;
	private final StockFallbackExecutor stockFallbackExecutor;
	private final RedisStockCircuitBreaker redisStockCircuitBreaker;

	public StockReservation reserve(final long eventProductId,
									final int totalStock,
									final long bookingId,
									final long userId) {
		if (!redisStockCircuitBreaker.allowRequest()) {
			return fallbackToDatabase(eventProductId, userId);
		}
		// sold-out 마커가 있으면 Lua 실행 전 빠르게 차단한다.
		if (redisClient.hasKey(soldOutKey(eventProductId))) {
			redisStockCircuitBreaker.recordSuccess();
			return StockReservation.redis(StockReservationResult.SOLD_OUT);
		}
		return redisClient.reserveStock(
						key(eventProductId),
						userKey(eventProductId),
						soldOutKey(eventProductId),
						totalStock,
						bookingId,
						userId,
						SOLD_OUT_MARKER_TTL_SECONDS
				)
				.map(scriptResult -> {
					redisStockCircuitBreaker.recordSuccess();
					return StockReservation.redis(toResult(scriptResult));
				})
				.orElseGet(() -> {
					redisStockCircuitBreaker.recordFailure();
					return fallbackToDatabase(eventProductId, userId);
				});
	}

	public void release(final long eventProductId,
						final long bookingId,
						final long userId,
						final StockReservationMethod reservationMethod) {
		if (reservationMethod == StockReservationMethod.DATABASE) {
			databaseStockReservationComponent.release(eventProductId);
			return;
		}
		// Redis 선점은 bookingId set + userId set을 함께 정리해야 1인 1개 제약이 해제된다.
		redisClient.removeSetMember(key(eventProductId), bookingId);
		redisClient.removeSetMember(userKey(eventProductId), userId);
	}

	public void release(final long eventProductId, final long bookingId, final long userId) {
		release(eventProductId, bookingId, userId, StockReservationMethod.REDIS);
	}

	private StockReservationResult toResult(final long scriptResult) {
		if (scriptResult == RESERVE_SUCCESS) {
			return StockReservationResult.SUCCESS;
		}
		if (scriptResult == RESERVE_SOLD_OUT) {
			return StockReservationResult.SOLD_OUT;
		}
		if (scriptResult == RESERVE_ALREADY_RESERVED) {
			return StockReservationResult.ALREADY_RESERVED;
		}
		if (scriptResult == RESERVE_DUPLICATED) {
			return StockReservationResult.DUPLICATED;
		}
		return StockReservationResult.UNAVAILABLE;
	}

	private StockReservation fallbackToDatabase(final long eventProductId, final long userId) {
		return StockReservation.database(
				stockFallbackExecutor.reserve(() -> databaseStockReservationComponent.reserve(eventProductId, userId))
		);
	}

	private String key(final long eventProductId) {
		return STOCK_USED_KEY_FORMAT.formatted(eventProductId);
	}

	private String userKey(final long eventProductId) {
		return STOCK_USER_KEY_FORMAT.formatted(eventProductId);
	}

	private String soldOutKey(final long eventProductId) {
		return STOCK_SOLD_OUT_KEY_FORMAT.formatted(eventProductId);
	}
}
