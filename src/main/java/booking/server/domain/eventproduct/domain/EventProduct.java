package booking.server.domain.eventproduct.domain;

import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventProduct(
		Long eventProductId,
		String name,
		BigDecimal price,
		int totalStock,
		LocalDateTime checkInAt,
		LocalDateTime checkOutAt,
		LocalDateTime openAt
) {

	public static EventProduct from(final Long eventProductId, final EventProductEntity eventProduct) {
		return new EventProduct(
				eventProductId,
				eventProduct.getName(),
				eventProduct.getPrice(),
				eventProduct.getTotalStock(),
				eventProduct.getCheckInAt(),
				eventProduct.getCheckOutAt(),
				eventProduct.getEventOpenAt()
		);
	}
}
