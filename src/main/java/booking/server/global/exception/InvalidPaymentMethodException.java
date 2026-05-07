package booking.server.global.exception;

public class InvalidPaymentMethodException extends BusinessException {

	public InvalidPaymentMethodException() {
		super(ErrorCode.INVALID_PAYMENT_METHOD);
	}
}
