package booking.server.global.exception;

public class SoldOutException extends BusinessException {

	public SoldOutException() {
		super(ErrorCode.SOLD_OUT);
	}
}
