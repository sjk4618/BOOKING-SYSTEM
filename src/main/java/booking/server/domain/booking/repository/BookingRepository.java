package booking.server.domain.booking.repository;

import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.booking.domain.entity.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {

	List<BookingEntity> findTop100ByStatusAndReservedUntilBefore(BookingStatus status, LocalDateTime reservedUntil);

	List<BookingEntity> findTop100ByStatus(BookingStatus status);
}
