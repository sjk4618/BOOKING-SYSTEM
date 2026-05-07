package booking.server.domain.idempotency.service;

public record IdempotencyCurrentPosition(
		boolean processingStarted,
		Integer httpStatus,
		Object responseBody
) {

	public static IdempotencyCurrentPosition processNewRequest() {
		return new IdempotencyCurrentPosition(true, null, null);
	}

	public static IdempotencyCurrentPosition replayStoredResponse(final int httpStatus, final Object responseBody) {
		return new IdempotencyCurrentPosition(false, httpStatus, responseBody);
	}

	public boolean hasStoredResponse() {
		return !processingStarted;
	}
}
