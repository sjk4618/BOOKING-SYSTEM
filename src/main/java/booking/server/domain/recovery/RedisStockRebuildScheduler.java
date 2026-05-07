package booking.server.domain.recovery;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.recovery.redis-rebuild.enabled", havingValue = "true")
public class RedisStockRebuildScheduler {

	private final RedisStockRebuildService redisStockRebuildService;

	@Scheduled(fixedDelayString = "${booking.recovery.redis-rebuild.delay-ms:60000}")
	public void rebuild() {
		redisStockRebuildService.rebuildAll();
	}
}
