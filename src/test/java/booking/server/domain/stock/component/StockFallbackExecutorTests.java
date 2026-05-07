package booking.server.domain.stock.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

@ExtendWith(MockitoExtension.class)
class StockFallbackExecutorTests {

	@Mock
	private AsyncTaskExecutor taskExecutor;

	private StockFallbackExecutor stockFallbackExecutor;

	@BeforeEach
	void setUp() {
		stockFallbackExecutor = new StockFallbackExecutor(taskExecutor);
	}

	@Test
	@DisplayName("전용 executor에서 DB fallback 예약을 실행한다")
	void reserve_executor실행_결과반환() {
		// given
		given(taskExecutor.submit(anyCallable()))
				.willReturn(CompletableFuture.completedFuture(StockReservationResult.SUCCESS));

		// when
		StockReservationResult result = stockFallbackExecutor.reserve(() -> StockReservationResult.SUCCESS);

		// then
		assertThat(result).isEqualTo(StockReservationResult.SUCCESS);
	}

	@Test
	@DisplayName("전용 executor가 포화되면 예약 불가를 반환한다")
	void reserve_executor포화_UNAVAILABLE() {
		// given
		given(taskExecutor.submit(anyCallable()))
				.willThrow(new RejectedExecutionException("full"));

		// when
		StockReservationResult result = stockFallbackExecutor.reserve(() -> StockReservationResult.SUCCESS);

		// then
		assertThat(result).isEqualTo(StockReservationResult.UNAVAILABLE);
	}

	@SuppressWarnings("unchecked")
	private Callable<StockReservationResult> anyCallable() {
		return any(Callable.class);
	}
}
