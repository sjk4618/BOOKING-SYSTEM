package booking.server.global.exception;

public class BadRequestException extends BusinessException {

	public BadRequestException() {
		super(ErrorCode.BAD_REQUEST);
	}
}
