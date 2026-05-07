package booking.server.domain.idempotency.repository;

import booking.server.domain.idempotency.domain.entity.IdempotencyEntity;
import booking.server.domain.idempotency.domain.entity.IdempotencyStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyEntity, Long> {

	Optional<IdempotencyEntity> findByIdempotencyKey(String idempotencyKey);

	List<IdempotencyEntity> findTop100ByStatusAndExpiresAtBefore(
			IdempotencyStatus status,
			LocalDateTime expiresAt
	);
}
