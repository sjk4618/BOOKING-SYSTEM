package booking.server.domain.recovery;

import booking.server.domain.idempotency.service.IdempotencyService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.recovery.idempotency.enabled", havingValue = "true")
public class IdempotencyExpiryScheduler {

	private final IdempotencyService idempotencyService;

	@Scheduled(fixedDelayString = "${booking.recovery.idempotency.delay-ms:60000}")
	public void expire() {
		idempotencyService.expireProcessing(LocalDateTime.now());
	}
}
