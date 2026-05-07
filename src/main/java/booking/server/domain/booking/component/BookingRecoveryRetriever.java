package booking.server.domain.booking.component;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import booking.server.domain.booking.repository.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRecoveryRetriever {

	private final BookingRepository bookingRepository;

	public List<BookingEntity> getExpiredPending(final LocalDateTime now) {
		return bookingRepository.findTop100ByStatusAndReservedUntilBefore(BookingStatus.PENDING, now);
	}

	public List<BookingEntity> getPaymentUnknown() {
		return bookingRepository.findTop100ByStatus(BookingStatus.PAYMENT_UNKNOWN);
	}
}
