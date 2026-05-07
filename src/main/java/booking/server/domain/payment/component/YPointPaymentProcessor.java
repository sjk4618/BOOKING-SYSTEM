package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class YPointPaymentProcessor implements PaymentProcessor {

	@Override
	public PaymentMethod paymentMethod() {
		return PaymentMethod.Y_POINT;
	}

	@Override
	public boolean requiresExternalApproval() {
		return false;
	}

	@Override
	public void approve(final String requestKey, final BigDecimal amount) {
	}
}
