package booking.server.domain.idempotency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import booking.server.domain.idempotency.component.IdempotencyResponseSerializer;
import booking.server.domain.idempotency.component.IdempotencyRetriever;
import booking.server.domain.idempotency.component.IdempotencySaver;
import booking.server.domain.idempotency.domain.Idempotency;
import booking.server.domain.idempotency.domain.entity.IdempotencyEntity;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTests {

	private static final String IDEMPOTENCY_KEY = "booking-key-1";
	private static final String REQUEST_BODY = "userId=10|eventProductId=1|payments=[CREDIT_CARD=100000]";

	@Mock
	private IdempotencyRetriever idempotencyRetriever;

	@Mock
	private IdempotencySaver idempotencySaver;

	@Mock
	private IdempotencyResponseSerializer responseSerializer;

	private IdempotencyService idempotencyService;

	@BeforeEach
	void setUp() {
		idempotencyService = new IdempotencyService(idempotencyRetriever, responseSerializer, idempotencySaver);
	}

	@Test
	@DisplayName("새 멱등키면 PROCESSING 상태를 생성한다")
	void checkOrCreateIdempotency_새멱등키_PROCESSING생성() {
		// given
		given(idempotencyRetriever.findByKey(IDEMPOTENCY_KEY)).willReturn(Optional.empty());
		given(idempotencySaver.save(any(IdempotencyEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		IdempotencyCurrentPosition result = idempotencyService.checkOrCreateIdempotency(IDEMPOTENCY_KEY, REQUEST_BODY);

		// then
		assertThat(result.processingStarted()).isTrue();
	}

	@Test
	@DisplayName("같은 멱등키의 성공 응답이 있으면 저장된 응답을 replay한다")
	void checkOrCreateIdempotency_성공응답존재_replay() {
		// given
		Object response = new Object();
		IdempotencyEntity existing = IdempotencyEntity.startProcessing(
				IDEMPOTENCY_KEY,
				REQUEST_BODY,
				LocalDateTime.now().plusHours(1)
		);
		existing.succeed("BOOKING", 100L, 201, "{\"bookingId\":100}");
		given(idempotencyRetriever.findByKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(Idempotency.fromEntity(existing)));
		given(responseSerializer.deserialize(201, "{\"bookingId\":100}")).willReturn(response);

		// when
		IdempotencyCurrentPosition result = idempotencyService.checkOrCreateIdempotency(IDEMPOTENCY_KEY, REQUEST_BODY);

		// then
		assertThat(result).isEqualTo(IdempotencyCurrentPosition.replayStoredResponse(201, response));
	}

	@Test
	@DisplayName("같은 멱등키로 다른 요청이 들어오면 422 예외를 던진다")
	void checkOrCreateIdempotency_요청본문다름_422예외() {
		// given
		IdempotencyEntity existing = IdempotencyEntity.startProcessing(
				IDEMPOTENCY_KEY,
				"userId=10|eventProductId=2|payments=[CREDIT_CARD=100000]",
				LocalDateTime.now().plusHours(1)
		);
		given(idempotencyRetriever.findByKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(Idempotency.fromEntity(existing)));

		// when & then
		assertThatThrownBy(() -> idempotencyService.checkOrCreateIdempotency(IDEMPOTENCY_KEY, REQUEST_BODY))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_MISMATCH));
	}

	@Test
	@DisplayName("같은 멱등키가 처리 중이면 처리 중 예외를 던진다")
	void checkOrCreateIdempotency_처리중_예외() {
		// given
		IdempotencyEntity existing = IdempotencyEntity.startProcessing(
				IDEMPOTENCY_KEY,
				REQUEST_BODY,
				LocalDateTime.now().plusHours(1)
		);
		given(idempotencyRetriever.findByKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(Idempotency.fromEntity(existing)));

		// when & then
		assertThatThrownBy(() -> idempotencyService.checkOrCreateIdempotency(IDEMPOTENCY_KEY, REQUEST_BODY))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_PROCESSING));
	}

	@Test
	@DisplayName("성공 응답을 저장한다")
	void succeed_성공응답저장() {
		// given
		Object response = new Object();
		IdempotencyEntity existing = IdempotencyEntity.startProcessing(
				IDEMPOTENCY_KEY,
				REQUEST_BODY,
				LocalDateTime.now().plusHours(1)
		);
		given(idempotencyRetriever.findEntityByKey(IDEMPOTENCY_KEY)).willReturn(Optional.of(existing));
		given(responseSerializer.serialize(response)).willReturn("{}");

		// when
		idempotencyService.succeed(IDEMPOTENCY_KEY, 100L, 201, response);

		// then
		assertThat(existing.getHttpStatus()).isEqualTo(201);
		then(responseSerializer).should().serialize(response);
	}
}
