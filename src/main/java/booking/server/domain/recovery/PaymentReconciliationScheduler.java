package booking.server.domain.recovery;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.recovery.payment-reconciliation.enabled", havingValue = "true")
public class PaymentReconciliationScheduler {

	@Scheduled(fixedDelayString = "${booking.recovery.payment-reconciliation.delay-ms:60000}")
	public void reconcile() {
		// Actual PG lookup is intentionally omitted until a real PG adapter exists.
	}
}
