package booking.server.domain.booking.dto;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BookingPaymentRequest(
		@Schema(description = "결제 수단", example = "CREDIT_CARD", allowableValues = {"CREDIT_CARD", "Y_PAY", "Y_POINT"})
		@NotNull PaymentMethod paymentMethod,
		@Schema(description = "결제 금액", example = "50000")
		@NotNull @Positive BigDecimal amount
) {
}
