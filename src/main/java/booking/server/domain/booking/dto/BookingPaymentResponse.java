package booking.server.domain.booking.dto;

import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record BookingPaymentResponse(
		@Schema(description = "결제 ID", example = "1")
		long paymentId,
		@Schema(description = "결제 수단", example = "CREDIT_CARD")
		PaymentMethod paymentMethod,
		@Schema(description = "결제 금액", example = "50000")
		BigDecimal amount,
		@Schema(description = "결제 상태", example = "COMPLETED")
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
