package booking.server.domain.eventproduct.component;

import static org.mockito.BDDMockito.then;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class EventProductCacheUpdaterTests {

	@Mock
	private EventProductCache eventProductCache;

	@InjectMocks
	private EventProductCacheUpdater eventProductCacheUpdater;

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	@DisplayName("트랜잭션 동기화가 없으면 즉시 Redis 캐시를 갱신한다")
	void updateAfterCommit_트랜잭션없음_즉시캐시갱신() {
		// given
		EventProductEntity eventProduct = eventProduct(1L);

		// when
		eventProductCacheUpdater.updateAfterCommit(eventProduct);

		// then
		then(eventProductCache).should().put(EventProduct.fromEntity(1L, eventProduct));
	}

	@Test
	@DisplayName("트랜잭션 동기화가 있으면 commit 이후 Redis 캐시를 갱신한다")
	void updateAfterCommit_트랜잭션있음_commit후캐시갱신() {
		// given
		EventProductEntity eventProduct = eventProduct(1L);
		TransactionSynchronizationManager.initSynchronization();

		// when
		eventProductCacheUpdater.updateAfterCommit(eventProduct);

		// then
		then(eventProductCache).shouldHaveNoInteractions();
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}
		then(eventProductCache).should().put(EventProduct.fromEntity(1L, eventProduct));
	}

	private EventProductEntity eventProduct(final Long id) {
		EventProductEntity eventProduct = EventProductEntity.create(
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				10,
				0,
				LocalDateTime.of(2026, 6, 1, 15, 0),
				LocalDateTime.of(2026, 6, 2, 11, 0),
				LocalDateTime.of(2026, 5, 10, 10, 0)
		);
		ReflectionTestUtils.setField(eventProduct, "id", id);
		return eventProduct;
	}
}
