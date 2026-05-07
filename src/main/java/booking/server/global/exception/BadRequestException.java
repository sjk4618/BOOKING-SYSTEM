package booking.server.global.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {

	private final ErrorCode errorCode;

	public BadRequestException(final ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
