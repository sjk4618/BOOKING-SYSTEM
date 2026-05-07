package booking.server.domain.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckoutUserResponse(
		@Schema(description = "사용자 ID", example = "1")
		long userId,
		@Schema(description = "사용자 이름", example = "Postman User")
		String name,
		@Schema(description = "사용 가능한 포인트", example = "100000")
		int availablePoint
) {
}
