package booking.server.domain.payment.component;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import booking.server.domain.booking.dto.BookingPaymentRequest;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.exception.InvalidPaymentAmountException;
import booking.server.domain.payment.exception.InvalidPaymentMethodException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentValidatorTests {

	private final PaymentValidator paymentValidator = new PaymentValidator();

	@Test
	@DisplayName("신용카드와 Y페이를 함께 요청하면 결제 검증에 실패한다")
	void validate_신용카드와Y페이혼용_예외() {
		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(BigDecimal.valueOf(100_000), List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(50_000)),
				new BookingPaymentRequest(PaymentMethod.Y_PAY, BigDecimal.valueOf(50_000))
		))).isInstanceOf(InvalidPaymentMethodException.class);
	}

	@Test
	@DisplayName("신용카드와 포인트 조합은 허용한다")
	void validate_신용카드와포인트_성공() {
		// when & then
		assertThatCode(() -> paymentValidator.validate(BigDecimal.valueOf(100_000), List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(90_000)),
				new BookingPaymentRequest(PaymentMethod.Y_POINT, BigDecimal.valueOf(10_000))
		))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Y페이와 포인트 조합은 허용한다")
	void validate_Y페이와포인트_성공() {
		// when & then
		assertThatCode(() -> paymentValidator.validate(BigDecimal.valueOf(100_000), List.of(
				new BookingPaymentRequest(PaymentMethod.Y_PAY, BigDecimal.valueOf(90_000)),
				new BookingPaymentRequest(PaymentMethod.Y_POINT, BigDecimal.valueOf(10_000))
		))).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("결제 금액 합계가 상품 가격과 다르면 요청 검증에 실패한다")
	void validate_결제금액불일치_예외() {
		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(BigDecimal.valueOf(100_000), List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(90_000))
		))).isInstanceOf(InvalidPaymentAmountException.class);
	}

	@Test
	@DisplayName("포인트 금액에 소수점이 있으면 금액 검증에 실패한다")
	void pointAmount_소수점포인트_예외() {
		// when & then
		assertThatThrownBy(() -> paymentValidator.validate(BigDecimal.valueOf(100_000), List.of(
				new BookingPaymentRequest(PaymentMethod.CREDIT_CARD, BigDecimal.valueOf(99_999.5)),
				new BookingPaymentRequest(PaymentMethod.Y_POINT, BigDecimal.valueOf(0.5))
		))).isInstanceOf(InvalidPaymentAmountException.class);
	}
}
