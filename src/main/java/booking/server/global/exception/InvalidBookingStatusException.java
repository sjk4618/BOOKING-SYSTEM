package booking.server.global.exception;

public class InvalidBookingStatusException extends BusinessException {

	public InvalidBookingStatusException() {
		super(ErrorCode.INVALID_BOOKING_STATUS);
	}
}
