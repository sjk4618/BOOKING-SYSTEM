package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class YPayPaymentProcessor implements PaymentProcessor {

	@Override
	public PaymentMethod paymentMethod() {
		return PaymentMethod.Y_PAY;
	}

	@Override
	public void approve(final String requestKey, final BigDecimal amount) {
	}
}
