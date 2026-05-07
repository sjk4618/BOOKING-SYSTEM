package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentMethod;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class YPayPaymentGateway implements PaymentGateway {

	@Override
	public PaymentMethod paymentMethod() {
		return PaymentMethod.Y_PAY;
	}

	@Override
	public void approve(final String requestKey, final BigDecimal amount) {
		// 실제 PG 승인 API를 호출하는 지점입니다. 현재는 local mock adapter로 성공 처리합니다.
	}

	@Override
	public void compensate(final String requestKey, final BigDecimal amount) {
		// 실제 PG 취소 API를 호출하는 지점입니다. 현재는 local mock adapter로 성공 처리합니다.
	}
}
