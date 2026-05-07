package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRetriever {

	private final PaymentRepository paymentRepository;

	public List<PaymentEntity> getByBookingId(final long bookingId) {
		return paymentRepository.findByBookingId(bookingId);
	}
}
