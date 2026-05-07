package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import java.math.BigDecimal;

public interface PaymentGateway {

	PaymentMethod paymentMethod();

	void approve(String requestKey, BigDecimal amount);

	void compensate(String requestKey, BigDecimal amount);
}
