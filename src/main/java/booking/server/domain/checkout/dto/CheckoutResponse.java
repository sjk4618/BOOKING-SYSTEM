package booking.server.domain.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckoutResponse(
		@Schema(description = "이벤트 상품 ID", example = "1")
		long eventProductId,
		@Schema(description = "상품명", example = "Postman Hotel Package")
		String name,
		@Schema(description = "상품 가격", example = "50000")
		BigDecimal price,
		@Schema(description = "체크인 시각", example = "2026-06-01T15:00:00")
		LocalDateTime checkInAt,
		@Schema(description = "체크아웃 시각", example = "2026-06-02T11:00:00")
		LocalDateTime checkOutAt,
		@Schema(description = "예약 오픈 시각", example = "2026-05-10T00:00:00")
		LocalDateTime openAt,
		@Schema(description = "사용자 정보")
		CheckoutUserResponse user
) {
}
