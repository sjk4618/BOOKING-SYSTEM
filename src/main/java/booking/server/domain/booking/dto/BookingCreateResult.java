package booking.server.domain.booking.dto;

public record BookingCreateResult(
		int httpStatus,
		Object body
) {
}
