package booking.server.global.exception;

public record ErrorResponse(
		int code,
		String message
) {

	public static ErrorResponse from(final ErrorCode errorCode) {
		return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
	}

	public static ErrorResponse from(final ErrorCode errorCode, final String message) {
		return new ErrorResponse(errorCode.getCode(), message);
	}
}
