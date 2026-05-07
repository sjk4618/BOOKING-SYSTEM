package booking.server.domain.stock.component;

import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisStockCircuitBreaker {

	private static final int FAILURE_THRESHOLD = 3;
	private static final Duration OPEN_DURATION = Duration.ofSeconds(10);

	private final Clock clock;
	private int failureCount = 0;
	private CircuitState state = CircuitState.CLOSED;
	private long openedAtMillis = 0L;

	public synchronized boolean allowRequest() {
		if (state == CircuitState.CLOSED) {
			return true;
		}
		if (state == CircuitState.HALF_OPEN) {
			return true;
		}
		if (clock.millis() - openedAtMillis >= OPEN_DURATION.toMillis()) {
			state = CircuitState.HALF_OPEN;
			return true;
		}
		return false;
	}

	public synchronized void recordSuccess() {
		failureCount = 0;
		state = CircuitState.CLOSED;
		openedAtMillis = 0L;
	}

	public synchronized void recordFailure() {
		if (state == CircuitState.HALF_OPEN) {
			open();
			return;
		}
		failureCount++;
		if (failureCount >= FAILURE_THRESHOLD) {
			open();
		}
	}

	private void open() {
		state = CircuitState.OPEN;
		openedAtMillis = clock.millis();
	}

	private enum CircuitState {
		CLOSED,
		OPEN,
		HALF_OPEN
	}
}
