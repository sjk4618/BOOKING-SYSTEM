package booking.server.domain.payment.component;

import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.repository.PaymentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSaver {

	private final PaymentRepository paymentRepository;

	public List<PaymentEntity> saveAll(final List<PaymentEntity> payments) {
		return paymentRepository.saveAll(payments);
	}
}
