package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.exception.PaymentFailedException;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorComposite {

	private final Map<PaymentMethod, PaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);

	public PaymentProcessorComposite(final List<PaymentProcessor> paymentProcessors) {
		for (PaymentProcessor processor : paymentProcessors) {
			processors.put(processor.paymentMethod(), processor);
		}
	}

	public void approve(final PaymentMethod paymentMethod, final String requestKey, final BigDecimal amount) {
		PaymentProcessor processor = processors.get(paymentMethod);
		if (processor == null) {
			throw new PaymentFailedException();
		}
		processor.prepare(requestKey, amount);
		if (!processor.requiresExternalApproval()) {
			return;
		}
		processor.approve(requestKey, amount);
	}

	public void compensate(final PaymentMethod paymentMethod, final String requestKey, final BigDecimal amount) {
		PaymentProcessor processor = processors.get(paymentMethod);
		if (processor == null || !processor.requiresExternalApproval()) {
			return;
		}
		processor.compensate(requestKey, amount);
	}
}
