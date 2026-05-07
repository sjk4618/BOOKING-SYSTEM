package booking.server.domain.idempotency.service;

import booking.server.domain.idempotency.component.IdempotencyResponseSerializer;
import booking.server.domain.idempotency.component.IdempotencyRetriever;
import booking.server.domain.idempotency.component.IdempotencySaver;
import booking.server.domain.idempotency.domain.Idempotency;
import booking.server.domain.idempotency.domain.entity.IdempotencyEntity;
import booking.server.domain.idempotency.domain.entity.IdempotencyStatus;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

	private static final int EXPIRES_HOURS = 24;

	private final IdempotencyRetriever idempotencyRetriever;
	private final IdempotencyResponseSerializer responseSerializer;
	private final IdempotencySaver idempotencySaver;

	@Transactional
	public IdempotencyCurrentPosition checkOrCreateIdempotency(final String idempotencyKey, final String requestBody) {
		final Optional<Idempotency> idempotency = idempotencyRetriever.findByKey(idempotencyKey);
		if (idempotency.isPresent()) {
			return handleExisting(idempotency.get(), requestBody);
		}
		try {
			idempotencySaver.save(IdempotencyEntity.startProcessing(
					idempotencyKey,
					requestBody,
					LocalDateTime.now().plusHours(EXPIRES_HOURS)
			));
			return IdempotencyCurrentPosition.processNewRequest();
		} catch (DataIntegrityViolationException e) {
			final Idempotency concurrentExisting = idempotencyRetriever.findByKey(idempotencyKey)
					.orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_PROCESSING));
			return handleExisting(concurrentExisting, requestBody);
		}
	}

	@Transactional
	public void succeed(final String idempotencyKey,
						final long resourceId,
						final int httpStatus,
						final Object response) {
		IdempotencyEntity idempotency = get(idempotencyKey);
		idempotency.succeed("BOOKING", resourceId, httpStatus, responseSerializer.serialize(response));
	}

	@Transactional
	public void fail(final String idempotencyKey, final int httpStatus, final Object response) {
		IdempotencyEntity idempotency = get(idempotencyKey);
		idempotency.fail(httpStatus, responseSerializer.serialize(response));
	}

	@Transactional
	public void expireProcessing(final LocalDateTime now) {
		idempotencyRetriever.findExpiredProcessing(now).forEach(IdempotencyEntity::expire);
	}

	private IdempotencyCurrentPosition handleExisting(final Idempotency idempotency, final String requestBody) {
		if (!idempotency.requestBody().equals(requestBody)) {
			throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_MISMATCH);
		}
		if (idempotency.status() == IdempotencyStatus.PROCESSING) {
			throw new BusinessException(ErrorCode.IDEMPOTENCY_PROCESSING);
		}
		if (idempotency.status() == IdempotencyStatus.SUCCEEDED || idempotency.status() == IdempotencyStatus.FAILED) {
			return IdempotencyCurrentPosition.replayStoredResponse(
					idempotency.httpStatus(),
					responseSerializer.deserialize(idempotency.httpStatus(), idempotency.responseBody())
			);
		}
		throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
	}

	private IdempotencyEntity get(final String idempotencyKey) {
		return idempotencyRetriever.findEntityByKey(idempotencyKey)
				.orElseThrow(() -> new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT));
	}
}
