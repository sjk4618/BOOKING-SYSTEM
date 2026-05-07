package booking.server.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisClient {

	private static final String RESERVE_STOCK_SCRIPT = """
				-- result: 1=success, 0=sold_out, 2=already_reserved(user), 3=duplicated(booking)
				local used = redis.call('SCARD', KEYS[1])
				local total = tonumber(ARGV[1])
				local bookingId = ARGV[2]
				local userId = ARGV[3]
				local soldOutTtlSeconds = tonumber(ARGV[4])

				if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
					return 2
				end

				if used >= total then
					redis.call('SET', KEYS[3], '1', 'EX', soldOutTtlSeconds)
					return 0
				end

				local added = redis.call('SADD', KEYS[1], bookingId)

				if added == 0 then
					return 3
				end

				local userAdded = redis.call('SADD', KEYS[2], userId)

				if userAdded == 0 then
					redis.call('SREM', KEYS[1], bookingId)
					return 2
				end

				if used + 1 >= total then
					redis.call('SET', KEYS[3], '1', 'EX', soldOutTtlSeconds)
				end

				return 1
				""";

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public <T> Optional<T> getJson(final String key, final Class<T> type) {
		try {
			String cached = redisTemplate.opsForValue().get(key);
			if (cached == null) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(cached, type));
		} catch (RuntimeException | JsonProcessingException exception) {
			return Optional.empty();
		}
	}

	public void setJson(final String key, final Object value, final Duration ttl) {
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
		} catch (RuntimeException | JsonProcessingException ignored) {
		}
	}

	public Optional<Long> getSetSize(final String key) {
		try {
			Long size = redisTemplate.opsForSet().size(key);
			if (size == null) {
				return Optional.of(0L);
			}
			return Optional.of(size);
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public Optional<Long> reserveStock(final String key,
									   final String userKey,
									   final String soldOutKey,
									   final int totalStock,
									   final long bookingId,
									   final long userId,
									   final long soldOutTtlSeconds) {
		try {
			DefaultRedisScript<Long> script = new DefaultRedisScript<>(RESERVE_STOCK_SCRIPT, Long.class);
			Long result = redisTemplate.execute(
					script,
					List.of(key, userKey, soldOutKey),
					String.valueOf(totalStock),
					String.valueOf(bookingId),
					String.valueOf(userId),
					String.valueOf(soldOutTtlSeconds)
			);
			return Optional.ofNullable(result);
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	public boolean hasKey(final String key) {
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(key));
		} catch (RuntimeException exception) {
			return false;
		}
	}

	public boolean removeSetMember(final String key, final long value) {
		try {
			Long removed = redisTemplate.opsForSet().remove(key, String.valueOf(value));
			return removed != null && removed > 0;
		} catch (RuntimeException exception) {
			return false;
		}
	}

	public void replaceSetMembers(final String key, final List<Long> values) {
		try {
			redisTemplate.delete(key);
			if (!values.isEmpty()) {
				String[] members = values.stream()
						.map(String::valueOf)
						.toArray(String[]::new);
				redisTemplate.opsForSet().add(key, members);
			}
		} catch (RuntimeException ignored) {
		}
	}

	public boolean tryLock(final String key, final Duration ttl) {
		try {
			return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", ttl));
		} catch (RuntimeException exception) {
			return false;
		}
	}

	public void unlock(final String key) {
		try {
			redisTemplate.delete(key);
		} catch (RuntimeException ignored) {
		}
	}
}
