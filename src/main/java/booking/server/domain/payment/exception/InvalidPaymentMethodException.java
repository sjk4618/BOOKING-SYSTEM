package booking.server.domain.payment.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class InvalidPaymentMethodException extends BusinessException {

	public InvalidPaymentMethodException() {
		super(ErrorCode.INVALID_PAYMENT_METHOD);
	}
}
