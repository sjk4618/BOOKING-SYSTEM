package booking.server.domain.payment.exception;

import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;

public class PointNotEnoughException extends BusinessException {

	public PointNotEnoughException() {
		super(ErrorCode.POINT_NOT_ENOUGH);
	}
}
