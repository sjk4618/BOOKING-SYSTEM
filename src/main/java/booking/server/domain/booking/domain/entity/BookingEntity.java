package booking.server.domain.booking.domain.entity;

import booking.server.domain.common.BaseTimeEntity;
import booking.server.domain.stock.domain.entity.StockReservationMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "bookings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_product_id", nullable = false)
	private long eventProductEntityId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BookingStatus status;

	@Column(name = "total_amount", nullable = false, scale = 0)
	private BigDecimal totalAmount;

	@Column(name = "reserved_until", nullable = false)
	private LocalDateTime reservedUntil;

	@Enumerated(EnumType.STRING)
	@Column(name = "stock_reservation_method", nullable = false, length = 20)
	private StockReservationMethod stockReservationMethod;

	private BookingEntity(final long eventProductEntityId,
						  final long userId,
						  final BookingStatus status,
						  final BigDecimal totalAmount,
						  final LocalDateTime reservedUntil,
						  final StockReservationMethod stockReservationMethod) {
		this.eventProductEntityId = eventProductEntityId;
		this.userId = userId;
		this.status = status;
		this.totalAmount = totalAmount;
		this.reservedUntil = reservedUntil;
		this.stockReservationMethod = stockReservationMethod;
	}

	public static BookingEntity create(final long eventProductEntityId,
									   final long userId,
									   final BigDecimal totalAmount,
									   final LocalDateTime reservedUntil,
									   final StockReservationMethod stockReservationMethod) {
		return new BookingEntity(
				eventProductEntityId,
				userId,
				BookingStatus.PENDING,
				totalAmount,
				reservedUntil,
				stockReservationMethod
		);
	}

	public void confirm() {
		this.status = BookingStatus.CONFIRMED;
	}

	public void cancel() {
		this.status = BookingStatus.CANCELLED;
	}

	public void failPayment() {
		this.status = BookingStatus.PAYMENT_FAILED;
	}

	public void markPaymentUnknown() {
		this.status = BookingStatus.PAYMENT_UNKNOWN;
	}

	public void expire() {
		this.status = BookingStatus.EXPIRED;
	}

	public void changeStockReservationMethod(final StockReservationMethod stockReservationMethod) {
		this.stockReservationMethod = stockReservationMethod;
	}
}
