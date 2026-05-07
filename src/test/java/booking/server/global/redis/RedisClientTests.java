package booking.server.global.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import booking.server.domain.eventproduct.domain.EventProduct;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisClientTests {

	private static final String KEY = "checkout:event-product:1";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private SetOperations<String, String> setOperations;

	@InjectMocks
	private RedisClient redisClient;

	@Test
	@DisplayName("JSON value를 Redis에서 조회한다")
	void getJson_캐시있음_역직렬화반환() throws Exception {
		// given
		EventProduct snapshot = snapshot();
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.get(KEY)).willReturn("json");
		given(objectMapper.readValue("json", EventProduct.class)).willReturn(snapshot);

		// when
		Optional<EventProduct> result = redisClient.getJson(KEY, EventProduct.class);

		// then
		assertThat(result).contains(snapshot);
	}

	@Test
	@DisplayName("JSON value를 TTL과 함께 Redis에 저장한다")
	void setJson_TTL과함께저장() throws Exception {
		// given
		EventProduct snapshot = snapshot();
		Duration ttl = Duration.ofMinutes(10);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(objectMapper.writeValueAsString(snapshot)).willReturn("json");

		// when
		redisClient.setJson(KEY, snapshot, ttl);

		// then
		then(valueOperations).should().set(KEY, "json", ttl);
	}

	@Test
	@DisplayName("Set 크기를 조회한다")
	void getSetSize_Set크기반환() {
		// given
		given(redisTemplate.opsForSet()).willReturn(setOperations);
		given(setOperations.size("event-product:1:stock:used")).willReturn(3L);

		// when
		Optional<Long> result = redisClient.getSetSize("event-product:1:stock:used");

		// then
		assertThat(result).contains(3L);
	}

	@Test
	@DisplayName("Redis lock 획득에 성공하면 true를 반환한다")
	void tryLock_성공_true() {
		// given
		Duration ttl = Duration.ofSeconds(30);
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.setIfAbsent("lock:key", "1", ttl)).willReturn(true);

		// when
		boolean result = redisClient.tryLock("lock:key", ttl);

		// then
		assertThat(result).isTrue();
	}

	private EventProduct snapshot() {
		return new EventProduct(
				1L,
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				10,
				LocalDateTime.of(2026, 6, 1, 15, 0),
				LocalDateTime.of(2026, 6, 2, 11, 0),
				LocalDateTime.of(2026, 5, 10, 10, 0)
		);
	}
}
