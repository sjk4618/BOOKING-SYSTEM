package booking.server.domain.booking.dto;

import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import java.math.BigDecimal;

public record BookingPaymentResponse(
		long paymentId,
		PaymentMethod paymentMethod,
		BigDecimal amount,
		PaymentStatus status
) {

	public static BookingPaymentResponse from(final PaymentEntity payment) {
		return new BookingPaymentResponse(
				payment.getId(),
				payment.getPaymentMethod(),
				payment.getAmount(),
				payment.getStatus()
		);
	}
}
