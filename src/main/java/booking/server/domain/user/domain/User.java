package booking.server.domain.user.domain;

public record User(
		long userId,
		String name,
		int availablePoint
) {

	public static User fromEntity(final booking.server.domain.user.domain.entity.User user) {
		return new User(
				user.getId(),
				user.getName(),
				user.getAvailablePoint()
		);
	}
}
