package booking.server.domain.booking.entity;

import booking.server.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "bookings"
//        uniqueConstraints = {
//                @UniqueConstraint(name = "uk_bookings_idempotency_key", columnNames = "idempotency_key")
//        }
)
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

    private BookingEntity(final long eventProductEntityId,
                          final long userId,
                          final BookingStatus status,
                          final BigDecimal totalAmount,
                          final LocalDateTime reservedUntil) {
        this.eventProductEntityId = eventProductEntityId;
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.reservedUntil = reservedUntil;
    }

    public static BookingEntity create(final long eventProductEntityId,
                                       final long userId,
                                       final BigDecimal totalAmount,
                                       final LocalDateTime reservedUntil) {
        return new BookingEntity(
                eventProductEntityId,
                userId,
                BookingStatus.PENDING,
                totalAmount,
                reservedUntil
        );
    }
}
