package booking.server.domain.payment.component;

import static org.mockito.BDDMockito.then;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCardPaymentProcessorTests {

	@Mock
	private CreditCardPaymentGateway paymentGateway;

	@InjectMocks
	private CreditCardPaymentProcessor paymentProcessor;

	@Test
	@DisplayName("신용카드 승인은 PG gateway로 위임한다")
	void approve_신용카드승인_gateway위임() {
		// given
		String requestKey = "payment-request-key";
		BigDecimal amount = BigDecimal.valueOf(100_000);

		// when
		paymentProcessor.approve(requestKey, amount);

		// then
		then(paymentGateway).should().approve(requestKey, amount);
	}

	@Test
	@DisplayName("신용카드 보상은 PG gateway로 위임한다")
	void compensate_신용카드취소_gateway위임() {
		// given
		String requestKey = "payment-request-key";
		BigDecimal amount = BigDecimal.valueOf(100_000);

		// when
		paymentProcessor.compensate(requestKey, amount);

		// then
		then(paymentGateway).should().compensate(requestKey, amount);
	}
}
