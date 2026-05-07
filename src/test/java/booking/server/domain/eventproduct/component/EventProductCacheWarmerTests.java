package booking.server.domain.eventproduct.component;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import booking.server.domain.eventproduct.repository.EventProductRepository;
import booking.server.global.redis.RedisClient;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventProductCacheWarmerTests {

	private static final String WARM_UP_LOCK_KEY = "lock:checkout:event-product:warm-up";

	@Mock
	private EventProductRepository eventProductRepository;

	@Mock
	private EventProductCache eventProductCache;

	@Mock
	private RedisClient redisClient;

	@InjectMocks
	private EventProductCacheWarmer eventProductCacheWarmer;

	@Test
	@DisplayName("warm-up lock을 얻지 못하면 DB를 조회하지 않는다")
	void warmUp_락획득실패_DB조회안함() {
		// given
		given(redisClient.tryLock(WARM_UP_LOCK_KEY, Duration.ofSeconds(30))).willReturn(false);

		// when
		eventProductCacheWarmer.warmUp();

		// then
		then(eventProductRepository).should(never()).findAll();
	}

	@Test
	@DisplayName("warm-up lock을 얻으면 DB 상품을 Redis 캐시에 적재한다")
	void warmUp_락획득성공_상품캐시적재() {
		// given
		EventProductEntity eventProduct = eventProduct(1L);
		given(redisClient.tryLock(WARM_UP_LOCK_KEY, Duration.ofSeconds(30))).willReturn(true);
		given(eventProductRepository.findAll()).willReturn(List.of(eventProduct));

		// when
		eventProductCacheWarmer.warmUp();

		// then
		then(eventProductCache).should().put(EventProduct.fromEntity(1L, eventProduct));
		then(redisClient).should().unlock(WARM_UP_LOCK_KEY);
	}

	private EventProductEntity eventProduct(final Long id) {
		EventProductEntity eventProduct = EventProductEntity.create(
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				10,
				0,
				LocalDateTime.of(2026, 6, 1, 15, 0),
				LocalDateTime.of(2026, 6, 2, 11, 0),
				LocalDateTime.of(2026, 5, 10, 10, 0)
		);
		ReflectionTestUtils.setField(eventProduct, "id", id);
		return eventProduct;
	}
}
