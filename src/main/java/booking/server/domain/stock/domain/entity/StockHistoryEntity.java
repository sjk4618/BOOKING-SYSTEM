package booking.server.domain.stock.domain.entity;

import booking.server.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "stock_histories"
//        indexes = {
//                @Index(name = "idx_stock_histories_event_product_id", columnList = "event_product_id"),
//                @Index(name = "idx_stock_histories_booking_id", columnList = "booking_id"),
//                @Index(name = "idx_stock_histories_user_id", columnList = "user_id")
//        },
//        uniqueConstraints = {
//                @UniqueConstraint(
//                        name = "uk_stock_histories_booking_type",
//                        columnNames = {"event_product_id", "booking_id", "type"}
//                )
//        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockHistoryEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_product_id", nullable = false)
    private long eventProductId;

    @Column(name = "booking_id", nullable = false)
    private long bookingId;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(nullable = false, scale = 0)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockHistoryType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_method", nullable = false, length = 20)
    private StockReservationMethod reservationMethod;

    private StockHistoryEntity(final long eventProductId,
                               final long bookingId,
                               final long userId,
                               final BigDecimal price,
                               final StockHistoryType type,
                               final StockReservationMethod reservationMethod) {
        this.eventProductId = eventProductId;
        this.bookingId = bookingId;
        this.userId = userId;
        this.price = price;
        this.type = type;
        this.reservationMethod = reservationMethod;
    }

    public static StockHistoryEntity reserve(final long eventProductId,
                                             final long bookingId,
                                             final long userId,
                                             final BigDecimal price,
                                             final StockReservationMethod reservationMethod) {
        return new StockHistoryEntity(
                eventProductId,
                bookingId,
                userId,
                price,
                StockHistoryType.RESERVE,
                reservationMethod
        );
    }

    public static StockHistoryEntity release(final long eventProductId,
                                             final long bookingId,
                                             final long userId,
                                             final BigDecimal price,
                                             final StockReservationMethod reservationMethod) {
        return new StockHistoryEntity(
                eventProductId,
                bookingId,
                userId,
                price,
                StockHistoryType.RELEASE,
                reservationMethod
        );
    }
}
