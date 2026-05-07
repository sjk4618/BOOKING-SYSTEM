package booking.server.domain.recovery;

import booking.server.domain.booking.component.BookingRecoveryRetriever;
import booking.server.domain.booking.domain.entity.BookingEntity;
import booking.server.domain.payment.component.PaymentRetriever;
import booking.server.domain.payment.domain.entity.PaymentEntity;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.stock.component.StockHistorySaver;
import booking.server.domain.stock.domain.entity.StockHistoryEntity;
import booking.server.domain.user.component.UserPointManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingRecoveryService {

	private final BookingRecoveryRetriever bookingRecoveryRetriever;
	private final PaymentRetriever paymentRetriever;
	private final StockHistorySaver stockHistorySaver;
	private final UserPointManager userPointManager;

	@Transactional
	public List<ExpiredBookingRelease> expirePendingBookings(final LocalDateTime now) {
		List<ExpiredBookingRelease> releases = new ArrayList<>();
		for (BookingEntity booking : bookingRecoveryRetriever.getExpiredPending(now)) {
			List<PaymentEntity> payments = paymentRetriever.getByBookingId(booking.getId());
			int pointAmount = pointAmount(payments);
			if (pointAmount > 0) {
				userPointManager.restore(booking.getUserId(), pointAmount);
			}
			payments.forEach(PaymentEntity::fail);
			booking.expire();
			stockHistorySaver.save(StockHistoryEntity.release(
					booking.getEventProductEntityId(),
					booking.getId(),
					booking.getUserId(),
					booking.getTotalAmount(),
					booking.getStockReservationMethod()
			));
			releases.add(new ExpiredBookingRelease(
					booking.getEventProductEntityId(),
					booking.getId(),
					booking.getUserId(),
					booking.getStockReservationMethod()
			));
		}
		return releases;
	}

	private int pointAmount(final List<PaymentEntity> payments) {
		return payments.stream()
				.filter(payment -> payment.getPaymentMethod() == PaymentMethod.Y_POINT)
				.map(PaymentEntity::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.intValueExact();
	}
}
