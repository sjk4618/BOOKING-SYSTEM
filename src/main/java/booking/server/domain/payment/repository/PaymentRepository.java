package booking.server.domain.payment.repository;

import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

	List<PaymentEntity> findByBookingId(long bookingId);

	List<PaymentEntity> findTop100ByStatus(PaymentStatus status);
}
