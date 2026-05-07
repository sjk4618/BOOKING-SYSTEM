package booking.server.domain.booking.dto;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BookingPaymentRequest(
		@NotNull PaymentMethod paymentMethod,
		@NotNull @Positive BigDecimal amount
) {
}
