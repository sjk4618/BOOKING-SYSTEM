package booking.server.domain.idempotency.domain.entity;

import booking.server.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
		name = "idempotency_keys",
		indexes = {
				@Index(name = "idx_idempotency_keys_expires_at", columnList = "expires_at")
		},
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_idempotency_keys_key", columnNames = "idempotency_key")
		}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "idempotency_key", nullable = false, length = 100)
	private String idempotencyKey;

	@Lob
	@Column(name = "request_body", nullable = false)
	private String requestBody;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private IdempotencyStatus status;

	@Column(name = "resource_type", length = 30)
	private String resourceType;

	@Column(name = "resource_id")
	private Long resourceId;

	@Column(name = "http_status")
	private Integer httpStatus;

	@Lob
	@Column(name = "response_body")
	private String responseBody;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	private IdempotencyEntity(final String idempotencyKey,
							  final String requestBody,
							  final LocalDateTime expiresAt) {
		this.idempotencyKey = idempotencyKey;
		this.requestBody = requestBody;
		this.status = IdempotencyStatus.PROCESSING;
		this.expiresAt = expiresAt;
	}

	public static IdempotencyEntity startProcessing(final String idempotencyKey,
													final String requestBody,
													final LocalDateTime expiresAt) {
		return new IdempotencyEntity(idempotencyKey, requestBody, expiresAt);
	}

	public boolean hasDifferentRequestBody(final String requestBody) {
		return !this.requestBody.equals(requestBody);
	}

	public void succeed(final String resourceType,
						final long resourceId,
						final int httpStatus,
						final String responseBody) {
		this.status = IdempotencyStatus.SUCCEEDED;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.httpStatus = httpStatus;
		this.responseBody = responseBody;
	}

	public void fail(final int httpStatus, final String responseBody) {
		this.status = IdempotencyStatus.FAILED;
		this.httpStatus = httpStatus;
		this.responseBody = responseBody;
	}

	public void expire() {
		this.status = IdempotencyStatus.EXPIRED;
	}
}
