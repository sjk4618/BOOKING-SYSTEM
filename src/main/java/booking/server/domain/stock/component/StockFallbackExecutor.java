package booking.server.domain.stock.component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class StockFallbackExecutor {

	private static final long FALLBACK_TIMEOUT_MILLIS = 2_000L;

	private final AsyncTaskExecutor taskExecutor;

	public StockFallbackExecutor(@Qualifier("stockFallbackTaskExecutor") final AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public StockReservationResult reserve(final Supplier<StockReservationResult> supplier) {
		Future<StockReservationResult> future;
		try {
			future = taskExecutor.submit(supplier::get);
		} catch (RejectedExecutionException exception) {
			return StockReservationResult.UNAVAILABLE;
		}
		try {
			return future.get(FALLBACK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return StockReservationResult.UNAVAILABLE;
		} catch (ExecutionException | TimeoutException exception) {
			future.cancel(true);
			return StockReservationResult.UNAVAILABLE;
		}
	}
}
