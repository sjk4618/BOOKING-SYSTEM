package booking.server.domain.eventproduct.repository;

import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventProductRepository extends JpaRepository<EventProductEntity, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update EventProductEntity e
			set e.usedStock = e.usedStock + 1
			where e.id = :eventProductId
				and e.usedStock < e.totalStock
			""")
	int increaseUsedStockIfAvailable(@Param("eventProductId") long eventProductId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update EventProductEntity e
			set e.usedStock = e.usedStock - 1
			where e.id = :eventProductId
				and e.usedStock > 0
			""")
	int decreaseUsedStock(@Param("eventProductId") long eventProductId);
}
