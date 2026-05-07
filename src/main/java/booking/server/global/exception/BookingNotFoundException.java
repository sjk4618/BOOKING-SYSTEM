package booking.server.global.exception;

public class BookingNotFoundException extends BusinessException {

	public BookingNotFoundException() {
		super(ErrorCode.BOOKING_NOT_FOUND);
	}
}
