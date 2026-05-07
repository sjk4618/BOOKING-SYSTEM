package booking.server.domain.booking.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class InvalidBookingStatusException extends BusinessException {

	public InvalidBookingStatusException() {
		super(ErrorCode.INVALID_BOOKING_STATUS);
	}
}
