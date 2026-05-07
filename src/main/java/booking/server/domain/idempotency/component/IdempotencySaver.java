package booking.server.domain.idempotency.component;

import booking.server.domain.idempotency.domain.entity.IdempotencyEntity;
import booking.server.domain.idempotency.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencySaver {

	private final IdempotencyRepository idempotencyRepository;

	public IdempotencyEntity save(final IdempotencyEntity idempotency) {
		return idempotencyRepository.saveAndFlush(idempotency);
	}
}
