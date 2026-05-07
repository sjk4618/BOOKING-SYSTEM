package booking.server.domain.booking.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class BookingExpiredException extends BusinessException {

	public BookingExpiredException() {
		super(ErrorCode.BOOKING_EXPIRED);
	}
}
