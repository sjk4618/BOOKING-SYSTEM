package booking.server.domain.booking.dto;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import booking.server.domain.payment.domain.entity.PaymentEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
		@Schema(description = "예약 ID", example = "1")
		long bookingId,
		@Schema(description = "이벤트 상품 ID", example = "1")
		long eventProductId,
		@Schema(description = "사용자 ID", example = "1")
		long userId,
		@Schema(description = "예약 상태", example = "CONFIRMED")
		BookingStatus status,
		@Schema(description = "예약 총 금액", example = "50000")
		BigDecimal totalAmount,
		@Schema(description = "예약 만료 시각", example = "2026-05-10T00:03:00")
		LocalDateTime reservedUntil,
		@Schema(description = "결제 내역")
		List<BookingPaymentResponse> payments
) {

	public static BookingResponse from(final BookingEntity booking, final List<PaymentEntity> payments) {
		return new BookingResponse(
				booking.getId(),
				booking.getEventProductEntityId(),
				booking.getUserId(),
				booking.getStatus(),
				booking.getTotalAmount(),
				booking.getReservedUntil(),
				payments.stream()
						.map(BookingPaymentResponse::from)
						.toList()
		);
	}
}
