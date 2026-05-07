package booking.server.domain.checkout.dto;

public record CheckoutUserResponse(
		long userId,
		String name,
		int availablePoint
) {
}
