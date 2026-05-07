package booking.server.domain.booking.validation;

import booking.server.domain.booking.dto.BookingCreateRequest;
import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;

public class PaymentCombinationValidator implements ConstraintValidator<ValidPaymentCombination, BookingCreateRequest> {

	@Override
	public boolean isValid(final BookingCreateRequest request, final ConstraintValidatorContext context) {
		if (request == null || request.payments() == null) {
			return true;
		}
		List<PaymentMethod> methods = request.payments().stream()
				.filter(Objects::nonNull)
				.map(BookingPaymentRequest::paymentMethod)
				.filter(Objects::nonNull)
				.toList();
		// 요구사항: 신용카드와 Y페이는 혼용 불가
		return !(methods.contains(PaymentMethod.CREDIT_CARD) && methods.contains(PaymentMethod.Y_PAY));
	}
}
