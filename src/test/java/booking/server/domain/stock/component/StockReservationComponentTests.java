package booking.server.domain.stock.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import booking.server.domain.stock.domain.entity.StockReservationMethod;
import booking.server.global.redis.RedisClient;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockReservationComponentTests {

	private static final long EVENT_PRODUCT_ID = 1L;
	private static final int TOTAL_STOCK = 10;
	private static final long BOOKING_ID = 100L;
	private static final long USER_ID = 10L;

	@Mock
	private RedisClient redisClient;

	@Mock
	private DatabaseStockReservationComponent databaseStockReservationComponent;

	@Mock
	private StockFallbackExecutor stockFallbackExecutor;

	@Mock
	private RedisStockCircuitBreaker redisStockCircuitBreaker;

	private StockReservationComponent stockReservationComponent;

	@BeforeEach
	void setUp() {
		stockReservationComponent = new StockReservationComponent(
				redisClient,
				databaseStockReservationComponent,
				stockFallbackExecutor,
				redisStockCircuitBreaker
		);
	}

	@Test
	@DisplayName("Redis 선점이 성공하면 Redis 예약 결과를 반환한다")
	void reserve_Redis성공_Redis예약반환() {
		// given
		given(redisStockCircuitBreaker.allowRequest()).willReturn(true);
		given(redisClient.hasKey("event-product:1:stock:sold-out")).willReturn(false);
		given(redisClient.reserveStock(
				"event-product:1:stock:used",
				"event-product:1:stock:users",
				"event-product:1:stock:sold-out",
				TOTAL_STOCK,
				BOOKING_ID,
				USER_ID,
				30L
		)).willReturn(Optional.of(1L));

		// when
		StockReservation result = stockReservationComponent.reserve(EVENT_PRODUCT_ID, TOTAL_STOCK, BOOKING_ID, USER_ID);

		// then
		assertThat(result).isEqualTo(StockReservation.redis(StockReservationResult.SUCCESS));
		then(redisStockCircuitBreaker).should().recordSuccess();
	}

	@Test
	@DisplayName("Redis 선점이 실패하면 전용 executor를 통해 DB fallback으로 예약한다")
	void reserve_Redis실패_DBfallback() {
		// given
		given(redisStockCircuitBreaker.allowRequest()).willReturn(true);
		given(redisClient.hasKey("event-product:1:stock:sold-out")).willReturn(false);
		given(redisClient.reserveStock(anyString(), anyString(), anyString(), anyInt(), anyLong(), anyLong(), anyLong()))
				.willReturn(Optional.empty());
		given(stockFallbackExecutor.reserve(anySupplier())).willReturn(StockReservationResult.SUCCESS);

		// when
		StockReservation result = stockReservationComponent.reserve(EVENT_PRODUCT_ID, TOTAL_STOCK, BOOKING_ID, USER_ID);

		// then
		assertThat(result).isEqualTo(new StockReservation(StockReservationResult.SUCCESS, StockReservationMethod.DATABASE));
		then(redisStockCircuitBreaker).should().recordFailure();
	}

	@Test
	@DisplayName("Redis circuit이 OPEN이면 Redis를 호출하지 않고 DB fallback으로 예약한다")
	void reserve_circuitOpen_DBfallback() {
		// given
		given(redisStockCircuitBreaker.allowRequest()).willReturn(false);
		given(stockFallbackExecutor.reserve(anySupplier())).willReturn(StockReservationResult.SOLD_OUT);

		// when
		StockReservation result = stockReservationComponent.reserve(EVENT_PRODUCT_ID, TOTAL_STOCK, BOOKING_ID, USER_ID);

		// then
		assertThat(result).isEqualTo(new StockReservation(StockReservationResult.SOLD_OUT, StockReservationMethod.DATABASE));
		then(redisClient).should(never()).reserveStock(anyString(), anyString(), anyString(), anyInt(), anyLong(), anyLong(), anyLong());
	}

	@SuppressWarnings("unchecked")
	private Supplier<StockReservationResult> anySupplier() {
		return any(Supplier.class);
	}
}
