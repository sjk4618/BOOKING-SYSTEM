package booking.server.domain.user.repository;

import booking.server.domain.user.domain.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update User u
			set u.availablePoint = u.availablePoint - :pointAmount
			where u.id = :userId
				and u.availablePoint >= :pointAmount
			""")
	int deductPoint(@Param("userId") long userId, @Param("pointAmount") int pointAmount);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update User u
			set u.availablePoint = u.availablePoint + :pointAmount
			where u.id = :userId
			""")
	int restorePoint(@Param("userId") long userId, @Param("pointAmount") int pointAmount);
}
