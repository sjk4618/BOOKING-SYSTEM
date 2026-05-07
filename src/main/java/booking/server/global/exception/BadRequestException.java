package booking.server.global.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

	private final ErrorCode errorCode;

	public BadRequestException() {
		super(ErrorCode.BAD_REQUEST.getMessage());
		this.errorCode = ErrorCode.BAD_REQUEST;
	}
}
