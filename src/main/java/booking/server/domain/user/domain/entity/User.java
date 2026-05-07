package booking.server.domain.user.domain.entity;

import booking.server.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "available_point", nullable = false)
    private int availablePoint;

    private User(final String name, final int availablePoint) {
        this.name = name;
        this.availablePoint = availablePoint;
    }

    public static User create(final String name, final int availablePoint) {
        return new User(name, availablePoint);
    }
}
