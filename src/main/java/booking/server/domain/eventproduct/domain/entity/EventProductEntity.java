package booking.server.domain.eventproduct.domain.entity;

import booking.server.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "event_products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventProductEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, scale = 0)
    private BigDecimal price;

    @Column(name = "total_stock", nullable = false)
    private int totalStock;

    @Column(name = "used_stock", nullable = false)
    private int usedStock;

    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkInAt;

    @Column(name = "check_out", nullable = false)
    private LocalDateTime checkOutAt;

    @Column(name = "open_at", nullable = false)
    private LocalDateTime eventOpenAt;

    private EventProductEntity(final String name,
                               final BigDecimal price,
                               final int totalStock,
                               final int usedStock,
                               final LocalDateTime checkInAt,
                               final LocalDateTime checkOutAt,
                               final LocalDateTime eventOpenAt) {
        this.name = name;
        this.price = price;
        this.totalStock = totalStock;
        this.usedStock = usedStock;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.eventOpenAt = eventOpenAt;
    }

    public static EventProductEntity create(final String name,
                                            final BigDecimal price,
                                            final int totalStock,
                                            final int usedStock,
                                            final LocalDateTime checkInAt,
                                            final LocalDateTime checkOutAt,
                                            final LocalDateTime eventOpenAt) {
        return new EventProductEntity(name, price, totalStock, usedStock, checkInAt, checkOutAt, eventOpenAt);
    }

    public int getRemainStock() {
        return totalStock - usedStock;
    }
}
