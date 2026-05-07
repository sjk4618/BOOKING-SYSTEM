package booking.server.domain.recovery;

import booking.server.domain.stock.component.StockReservationComponent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.recovery.pending.enabled", havingValue = "true")
public class PendingBookingRecoveryScheduler {

	private final BookingRecoveryService bookingRecoveryService;
	private final StockReservationComponent stockReservationComponent;

	@Scheduled(fixedDelayString = "${booking.recovery.pending.delay-ms:30000}")
	public void recover() {
		bookingRecoveryService.expirePendingBookings(LocalDateTime.now())
				.forEach(release -> stockReservationComponent.release(
						release.eventProductId(),
						release.bookingId(),
						release.userId(),
						release.reservationMethod()
				));
	}
}
