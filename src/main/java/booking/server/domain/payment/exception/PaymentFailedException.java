package booking.server.domain.payment.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class PaymentFailedException extends BusinessException {

	public PaymentFailedException() {
		super(ErrorCode.PAYMENT_FAILED);
	}
}
