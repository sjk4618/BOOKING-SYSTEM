package booking.server.domain.payment.component;

import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.exception.InvalidPaymentAmountException;
import booking.server.domain.payment.exception.InvalidPaymentMethodException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PaymentValidator {

	public void validate(final BigDecimal price, final List<BookingPaymentRequest> payments) {
		validateNotEmpty(payments);
		final Set<PaymentMethod> methods = EnumSet.noneOf(PaymentMethod.class);
		BigDecimal totalAmount = BigDecimal.ZERO;
		for (BookingPaymentRequest payment : payments) {
			validatePayment(payment);
			if (!methods.add(payment.paymentMethod())) { // 동일한 결제수단은 중복 사용할 수 없다.
				throw new InvalidPaymentMethodException();
			}
			totalAmount = totalAmount.add(payment.amount());
		}

		// 신용카드와 Y페이는 동시에 사용할 수 없다.
		if (methods.contains(PaymentMethod.CREDIT_CARD) && methods.contains(PaymentMethod.Y_PAY)) {
			throw new InvalidPaymentMethodException();
		}

		validatePointAmount(payments);

		// 결제수단별 금액 합계는 상품 가격과 정확히 일치해야 한다.
		if (price.compareTo(totalAmount) != 0) {
			throw new InvalidPaymentAmountException();
		}
	}

	public int pointAmount(final List<BookingPaymentRequest> payments) {
		return payments.stream()
				.filter(payment -> payment.paymentMethod() == PaymentMethod.Y_POINT)
				.map(BookingPaymentRequest::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.intValueExact();
	}

	private void validatePointAmount(final List<BookingPaymentRequest> payments) {
		try {
			pointAmount(payments);
		} catch (ArithmeticException exception) {
			throw new InvalidPaymentAmountException();
		}
	}

	private void validateNotEmpty(final List<BookingPaymentRequest> payments) {
		if (payments == null || payments.isEmpty()) {
			throw new InvalidPaymentMethodException();
		}
	}

	private void validatePayment(final BookingPaymentRequest payment) {
		if (payment.paymentMethod() == null) {
			throw new InvalidPaymentMethodException();
		}

		if (payment.amount() == null || payment.amount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidPaymentAmountException();
		}
	}
}
