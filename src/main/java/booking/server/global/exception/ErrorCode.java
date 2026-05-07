package booking.server.global.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {

	/**
	 * 400 Bad Request
	 */
	BAD_REQUEST(HttpStatus.BAD_REQUEST, 40000, "잘못된 요청입니다."),

	/**
	 * 404 Not Found
	 */
	EVENT_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "상품을 찾을 수 없습니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, 40402, "사용자를 찾을 수 없습니다."),
	BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND, 40403, "예약을 찾을 수 없습니다."),

	/**
	 * 409 Conflict
	 */
	SOLD_OUT(HttpStatus.CONFLICT, 40901, "현재 예약 가능한 재고가 없습니다."),
	INVALID_BOOKING_STATUS(HttpStatus.CONFLICT, 40902, "예약 상태가 올바르지 않습니다."),
	BOOKING_EXPIRED(HttpStatus.CONFLICT, 40903, "예약 가능 시간이 만료되었습니다."),
	PAYMENT_FAILED(HttpStatus.CONFLICT, 40904, "결제에 실패했습니다."),
	INVALID_PAYMENT_METHOD(HttpStatus.CONFLICT, 40905, "결제 수단이 올바르지 않습니다."),
	POINT_NOT_ENOUGH(HttpStatus.CONFLICT, 40906, "포인트가 부족합니다."),

	/**
	 * 500 Internal Server Error
	 */
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버 내부 오류가 발생했습니다.");

	private final HttpStatus status;
	private final int code;
	private final String message;
}
