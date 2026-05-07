package booking.server.domain.payment.domain;

import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import java.math.BigDecimal;

public record Payment(
		long paymentId,
		long bookingId,
		PaymentMethod paymentMethod,
		BigDecimal amount,
		PaymentStatus status,
		String requestKey
) {

	public static Payment fromEntity(final PaymentEntity payment) {
		return new Payment(
				payment.getId(),
				payment.getBookingId(),
				payment.getPaymentMethod(),
				payment.getAmount(),
				payment.getStatus(),
				payment.getRequestKey()
		);
	}
}
