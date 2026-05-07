package booking.server.domain.checkout.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import booking.server.global.redis.RedisClient;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockUsageRetrieverTests {

	@Mock
	private RedisClient redisClient;

	@InjectMocks
	private StockUsageRetriever stockUsageRetriever;

	@Test
	@DisplayName("Redis Set 크기를 재고 사용량으로 반환한다")
	void get_RedisSet크기_사용량반환() {
		// given
		given(redisClient.getSetSize("event-product:1:stock:used")).willReturn(Optional.of(3L));

		// when
		StockUsageResult result = stockUsageRetriever.getStockUsage(1L);

		// then
		assertThat(result.redisAvailable()).isTrue();
		assertThat(result.usedCount()).isEqualTo(3L);
	}

	@Test
	@DisplayName("Redis 조회가 불가능하면 unavailable을 반환한다")
	void get_Redis조회불가_unavailable반환() {
		// given
		given(redisClient.getSetSize("event-product:1:stock:used")).willReturn(Optional.empty());

		// when
		StockUsageResult result = stockUsageRetriever.getStockUsage(1L);

		// then
		assertThat(result.redisAvailable()).isFalse();
	}
}
