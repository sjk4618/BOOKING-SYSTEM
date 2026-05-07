package booking.server.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
		@Schema(description = "서비스 에러 코드", example = "40901")
		int code,
		@Schema(description = "에러 메시지", example = "현재 예약 가능한 재고가 없습니다.")
		String message
) {

	public static ErrorResponse from(final ErrorCode errorCode) {
		return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
	}

	public static ErrorResponse from(final ErrorCode errorCode, final String message) {
		return new ErrorResponse(errorCode.getCode(), message);
	}
}
