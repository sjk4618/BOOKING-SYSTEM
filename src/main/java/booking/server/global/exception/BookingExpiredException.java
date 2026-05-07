package booking.server.global.exception;

public class BookingExpiredException extends BusinessException {

	public BookingExpiredException() {
		super(ErrorCode.BOOKING_EXPIRED);
	}
}
