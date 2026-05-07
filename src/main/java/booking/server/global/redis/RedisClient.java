package booking.server.global.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisClient {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisClient(final StringRedisTemplate redisTemplate, final ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

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
