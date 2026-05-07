package booking.server.domain.booking.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class SoldOutException extends BusinessException {

	public SoldOutException() {
		super(ErrorCode.SOLD_OUT);
	}
}
