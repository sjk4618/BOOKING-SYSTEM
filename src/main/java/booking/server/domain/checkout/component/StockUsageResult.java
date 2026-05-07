package booking.server.domain.checkout.component;

public record StockUsageResult(
		boolean redisAvailable,
		long usedCount
) {

	public static StockUsageResult success(final long usedCount) {
		return new StockUsageResult(true, usedCount);
	}

	public static StockUsageResult unavailable() {
		return new StockUsageResult(false, 0L);
	}
}
