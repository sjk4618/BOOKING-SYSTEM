package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class CreditCardPaymentProcessor implements PaymentProcessor {

	@Override
	public PaymentMethod paymentMethod() {
		return PaymentMethod.CREDIT_CARD;
	}

	@Override
	public void approve(final String requestKey, final BigDecimal amount) {
	}
}
