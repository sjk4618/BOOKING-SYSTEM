package booking.server.domain.booking.component;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.exception.BookingNotFoundException;
import booking.server.domain.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRetriever {

	private final BookingRepository bookingRepository;

	public BookingEntity get(final long bookingId) {
		return bookingRepository.findById(bookingId)
				.orElseThrow(BookingNotFoundException::new);
	}
}
