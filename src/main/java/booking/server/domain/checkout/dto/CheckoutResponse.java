package booking.server.domain.checkout.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckoutResponse(
		long eventProductId,
		String name,
		BigDecimal price,
		LocalDateTime checkInAt,
		LocalDateTime checkOutAt,
		LocalDateTime openAt,
		CheckoutUserResponse user
) {
}
