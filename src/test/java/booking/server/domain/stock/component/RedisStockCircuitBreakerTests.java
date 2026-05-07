package booking.server.domain.stock.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisStockCircuitBreakerTests {

	@Test
	@DisplayName("Redis 실패가 임계치에 도달하면 요청을 차단한다")
	void allowRequest_실패임계치도달_false() {
		// given
		MutableClock clock = new MutableClock();
		RedisStockCircuitBreaker circuitBreaker = new RedisStockCircuitBreaker(clock);
		circuitBreaker.recordFailure();
		circuitBreaker.recordFailure();
		circuitBreaker.recordFailure();

		// when
		boolean result = circuitBreaker.allowRequest();

		// then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("OPEN 시간이 지나면 Redis 요청을 다시 허용한다")
	void allowRequest_OPEN시간경과_true() {
		// given
		MutableClock clock = new MutableClock();
		RedisStockCircuitBreaker circuitBreaker = new RedisStockCircuitBreaker(clock);
		circuitBreaker.recordFailure();
		circuitBreaker.recordFailure();
		circuitBreaker.recordFailure();
		clock.advance(Duration.ofSeconds(10));

		// when
		boolean result = circuitBreaker.allowRequest();

		// then
		assertThat(result).isTrue();
	}

	@Test
	@DisplayName("Redis 성공을 기록하면 실패 횟수를 초기화한다")
	void recordSuccess_성공기록_요청허용() {
		// given
		MutableClock clock = new MutableClock();
		RedisStockCircuitBreaker circuitBreaker = new RedisStockCircuitBreaker(clock);
		circuitBreaker.recordFailure();
		circuitBreaker.recordFailure();
		circuitBreaker.recordSuccess();
		circuitBreaker.recordFailure();

		// when
		boolean result = circuitBreaker.allowRequest();

		// then
		assertThat(result).isTrue();
	}

	private static class MutableClock extends Clock {

		private Instant instant = Instant.parse("2026-05-07T00:00:00Z");

		private void advance(final Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("Asia/Seoul");
		}

		@Override
		public Clock withZone(final ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
