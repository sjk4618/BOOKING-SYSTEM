package booking.server.domain.booking.component;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingSaver {

	private final BookingRepository bookingRepository;

	public BookingEntity save(final BookingEntity booking) {
		return bookingRepository.save(booking);
	}
}
