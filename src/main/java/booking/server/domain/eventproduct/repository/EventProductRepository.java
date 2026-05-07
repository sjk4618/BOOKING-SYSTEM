package booking.server.domain.eventproduct.repository;

import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventProductRepository extends JpaRepository<EventProductEntity, Long> {
}
