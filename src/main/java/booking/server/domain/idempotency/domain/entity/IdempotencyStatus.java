package booking.server.domain.idempotency.domain.entity;

public enum IdempotencyStatus {
	PROCESSING,
	SUCCEEDED,
	FAILED,
	EXPIRED
}
