package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import java.math.BigDecimal;

public interface PaymentProcessor {

	PaymentMethod paymentMethod();

	default boolean requiresExternalApproval() {
		return true;
	}

	default void prepare(String requestKey, BigDecimal amount) {
	}

	void approve(String requestKey, BigDecimal amount);

	default void complete(String requestKey, BigDecimal amount) {
	}

	default void compensate(String requestKey, BigDecimal amount) {
	}
}
