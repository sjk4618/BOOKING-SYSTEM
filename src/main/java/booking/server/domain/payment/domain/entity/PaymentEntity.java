package booking.server.domain.payment.domain.entity;

import booking.server.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "request_key", nullable = false, length = 100)
    private String requestKey;

    private PaymentEntity(final long bookingId,
                          final PaymentMethod paymentMethod,
                          final BigDecimal amount,
                          final PaymentStatus status,
                          final String requestKey) {
        this.bookingId = bookingId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = status;
        this.requestKey = requestKey;
    }

    public static PaymentEntity create(final long bookingId,
                                       final PaymentMethod paymentMethod,
                                       final BigDecimal amount,
                                       final String requestKey) {
        return new PaymentEntity(bookingId, paymentMethod, amount, PaymentStatus.PENDING, requestKey);
    }
}
