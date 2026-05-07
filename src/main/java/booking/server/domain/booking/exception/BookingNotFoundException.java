package booking.server.domain.booking.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class BookingNotFoundException extends BusinessException {

	public BookingNotFoundException() {
		super(ErrorCode.BOOKING_NOT_FOUND);
	}
}
