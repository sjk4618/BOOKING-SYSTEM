package booking.server.domain.stock.repository;

import booking.server.domain.stock.domain.entity.StockHistoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockHistoryRepository extends JpaRepository<StockHistoryEntity, Long> {

	@Query("select distinct s.eventProductId from StockHistoryEntity s")
	List<Long> findDistinctEventProductIds();

	@Query("""
			select s.bookingId
			from StockHistoryEntity s
			where s.eventProductId = :eventProductId
			group by s.bookingId
			having
				sum(case when s.type = booking.server.domain.stock.domain.entity.StockHistoryType.RESERVE then 1 else 0 end) > 0
				and sum(case when s.type = booking.server.domain.stock.domain.entity.StockHistoryType.RELEASE then 1 else 0 end) = 0
			""")
	List<Long> findActiveBookingIdsByEventProductId(long eventProductId);

	@Query("""
			select s.userId
			from StockHistoryEntity s
			where s.eventProductId = :eventProductId
			group by s.userId
			having
				sum(case when s.type = booking.server.domain.stock.domain.entity.StockHistoryType.RESERVE then 1 else 0 end) > 0
				and sum(case when s.type = booking.server.domain.stock.domain.entity.StockHistoryType.RELEASE then 1 else 0 end) = 0
			""")
	List<Long> findActiveUserIdsByEventProductId(long eventProductId);

	@Query("""
			select count(s) > 0
			from StockHistoryEntity s
			where s.eventProductId = :eventProductId
				and s.userId = :userId
				and s.type = booking.server.domain.stock.domain.entity.StockHistoryType.RESERVE
				and not exists (
					select 1
					from StockHistoryEntity release
					where release.eventProductId = s.eventProductId
						and release.bookingId = s.bookingId
						and release.type = booking.server.domain.stock.domain.entity.StockHistoryType.RELEASE
				)
			""")
	boolean existsActiveReservationByEventProductIdAndUserId(long eventProductId, long userId);
}
