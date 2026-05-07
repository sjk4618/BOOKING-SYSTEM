package booking.server.global.exception;

public class PointNotEnoughException extends BusinessException {

	public PointNotEnoughException() {
		super(ErrorCode.POINT_NOT_ENOUGH);
	}
}
