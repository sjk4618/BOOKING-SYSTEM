package booking.server.domain.idempotency.domain;

import booking.server.domain.idempotency.domain.entity.IdempotencyEntity;
import booking.server.domain.idempotency.domain.entity.IdempotencyStatus;
import lombok.Getter;

import java.time.LocalDateTime;

public record Idempotency(
		Long idempotencyId,
		String idempotencyKey,
		String requestBody,
		IdempotencyStatus status,
		String resourceType,
		Long resourceId,
		Integer httpStatus,
		String responseBody,
		LocalDateTime expiresAt
) {

	public static Idempotency fromEntity(final IdempotencyEntity idempotency) {
		return new Idempotency(
				idempotency.getId(),
				idempotency.getIdempotencyKey(),
				idempotency.getRequestBody(),
				idempotency.getStatus(),
				idempotency.getResourceType(),
				idempotency.getResourceId(),
				idempotency.getHttpStatus(),
				idempotency.getResponseBody(),
				idempotency.getExpiresAt()
		);
	}
}
