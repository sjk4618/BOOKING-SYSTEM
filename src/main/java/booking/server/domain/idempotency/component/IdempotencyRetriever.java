package booking.server.domain.idempotency.component;

import booking.server.domain.idempotency.domain.Idempotency;
import booking.server.domain.idempotency.domain.entity.IdempotencyEntity;
import booking.server.domain.idempotency.domain.entity.IdempotencyStatus;
import booking.server.domain.idempotency.repository.IdempotencyRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyRetriever {

	private final IdempotencyRepository idempotencyRepository;

	public Optional<Idempotency> findByKey(final String idempotencyKey) {
		return idempotencyRepository.findByIdempotencyKey(idempotencyKey)
				.map(Idempotency::fromEntity);
	}

	public Optional<IdempotencyEntity> findEntityByKey(final String idempotencyKey) {
		return idempotencyRepository.findByIdempotencyKey(idempotencyKey);
	}

	public List<IdempotencyEntity> findExpiredProcessing(final LocalDateTime now) {
		return idempotencyRepository.findTop100ByStatusAndExpiresAtBefore(IdempotencyStatus.PROCESSING, now);
	}
}
