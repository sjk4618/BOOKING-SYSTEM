package booking.server.domain.eventproduct.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.domain.entity.EventProductEntity;
import booking.server.domain.eventproduct.exception.EventProductNotFoundException;
import booking.server.domain.eventproduct.repository.EventProductRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventProductRetrieverTests {

	private static final Long EVENT_PRODUCT_ID = 1L;
	private static final LocalDateTime CHECK_IN_AT = LocalDateTime.of(2026, 6, 1, 15, 0);
	private static final LocalDateTime CHECK_OUT_AT = LocalDateTime.of(2026, 6, 2, 11, 0);
	private static final LocalDateTime OPEN_AT = LocalDateTime.of(2026, 5, 10, 10, 0);

	@Mock
	private EventProductRepository eventProductRepository;

	@Mock
	private EventProductCache eventProductCache;

	@InjectMocks
	private EventProductRetriever eventProductRetriever;

	@Test
	@DisplayName("상품 캐시가 있으면 repository를 조회하지 않는다")
	void read_상품캐시Hit_repository조회안함() {
		// given
		EventProduct snapshot = productSnapshot();
		given(eventProductCache.getFromRedis(EVENT_PRODUCT_ID)).willReturn(Optional.of(snapshot));

		// when
		EventProduct result = eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID);

		// then
		assertThat(result).isEqualTo(snapshot);
		then(eventProductRepository).should(never()).findById(EVENT_PRODUCT_ID);
	}

	@Test
	@DisplayName("상품 캐시가 없으면 repository에서 조회하고 캐시에 저장한다")
	void read_상품캐시Miss_repository조회후캐시저장() {
		// given
		given(eventProductCache.getFromRedis(EVENT_PRODUCT_ID)).willReturn(Optional.empty());
		given(eventProductRepository.findById(EVENT_PRODUCT_ID)).willReturn(Optional.of(eventProduct()));

		// when
		EventProduct result = eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID);

		// then
		assertThat(result).isEqualTo(productSnapshot());
		then(eventProductCache).should().put(productSnapshot());
	}

	@Test
	@DisplayName("상품을 찾을 수 없으면 예외가 발생한다")
	void read_상품없음_예외() {
		// given
		given(eventProductCache.getFromRedis(EVENT_PRODUCT_ID)).willReturn(Optional.empty());
		given(eventProductRepository.findById(EVENT_PRODUCT_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID))
				.isInstanceOf(EventProductNotFoundException.class);
	}

	private EventProductEntity eventProduct() {
		return EventProductEntity.create(
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				10,
				0,
				CHECK_IN_AT,
				CHECK_OUT_AT,
				OPEN_AT
		);
	}

	private EventProduct productSnapshot() {
		return new EventProduct(
				EVENT_PRODUCT_ID,
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				10,
				CHECK_IN_AT,
				CHECK_OUT_AT,
				OPEN_AT
		);
	}
}
