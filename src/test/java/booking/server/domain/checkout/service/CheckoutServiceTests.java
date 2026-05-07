package booking.server.domain.checkout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import booking.server.domain.checkout.component.StockUsageRetriever;
import booking.server.domain.checkout.component.StockUsageResult;
import booking.server.domain.checkout.component.UserRetriever;
import booking.server.domain.checkout.dto.CheckoutResponse;
import booking.server.domain.eventproduct.component.EventProductRetriever;
import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.exception.EventProductNotFoundException;
import booking.server.domain.user.domain.entity.User;
import booking.server.domain.user.exception.UserNotFoundException;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTests {

	private static final Long EVENT_PRODUCT_ID = 1L;
	private static final Long USER_ID = 10L;
	private static final LocalDateTime CHECK_IN_AT = LocalDateTime.of(2026, 6, 1, 15, 0);
	private static final LocalDateTime CHECK_OUT_AT = LocalDateTime.of(2026, 6, 2, 11, 0);
	private static final LocalDateTime OPEN_AT = LocalDateTime.of(2026, 5, 10, 10, 0);

	@Mock
	private EventProductRetriever eventProductRetriever;

	@Mock
	private UserRetriever userRetriever;

	@Mock
	private StockUsageRetriever stockUsageRetriever;

	@InjectMocks
	private CheckoutService checkoutService;

	@Test
	@DisplayName("상품 캐시와 Redis 재고 조회가 성공하면 checkout 정보를 반환한다")
	void getCheckout_캐시와Redis재고조회성공_checkout반환() {
		// given
		given(eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID)).willReturn(productSnapshot(10));
		given(userRetriever.getUser(USER_ID)).willReturn(User.create("user", 5_000));
		given(stockUsageRetriever.getStockUsage(EVENT_PRODUCT_ID)).willReturn(StockUsageResult.success(7));

		// when
		CheckoutResponse response = checkoutService.getCheckout(EVENT_PRODUCT_ID, USER_ID);

		// then
		assertThat(response.eventProductId()).isEqualTo(EVENT_PRODUCT_ID);
		assertThat(response.name()).isEqualTo("Hotel Package A");
		assertThat(response.user().availablePoint()).isEqualTo(5_000);
	}

	@Test
	@DisplayName("Redis 재고 조회 결과 남은 재고가 없으면 품절 예외가 발생한다")
	void getCheckout_남은재고없음_품절예외() {
		// given
		given(eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID)).willReturn(productSnapshot(10));
		given(userRetriever.getUser(USER_ID)).willReturn(User.create("user", 5_000));
		given(stockUsageRetriever.getStockUsage(EVENT_PRODUCT_ID)).willReturn(StockUsageResult.success(10));

		// when & then
		assertThatThrownBy(() -> checkoutService.getCheckout(EVENT_PRODUCT_ID, USER_ID))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SOLD_OUT));
	}

	@Test
	@DisplayName("Redis 재고 조회가 실패하면 예약 단계 확인 상태로 checkout을 허용한다")
	void getCheckout_Redis재고조회실패_예약단계확인상태반환() {
		// given
		given(eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID)).willReturn(productSnapshot(10));
		given(userRetriever.getUser(USER_ID)).willReturn(User.create("user", 5_000));
		given(stockUsageRetriever.getStockUsage(EVENT_PRODUCT_ID)).willReturn(StockUsageResult.unavailable());

		// when
		CheckoutResponse response = checkoutService.getCheckout(EVENT_PRODUCT_ID, USER_ID);

		// then
		assertThat(response.eventProductId()).isEqualTo(EVENT_PRODUCT_ID);
		assertThat(response.name()).isEqualTo("Hotel Package A");
	}

	@Test
	@DisplayName("상품을 찾을 수 없으면 예외가 발생한다")
	void getCheckout_상품없음_예외() {
			// given
			given(eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID))
					.willThrow(new EventProductNotFoundException());

		// when & then
		assertThatThrownBy(() -> checkoutService.getCheckout(EVENT_PRODUCT_ID, USER_ID))
				.isInstanceOf(BusinessException.class)
				.hasMessage("상품을 찾을 수 없습니다.");
	}

	@Test
	@DisplayName("사용자를 찾을 수 없으면 예외가 발생한다")
	void getCheckout_사용자없음_예외() {
			// given
			given(eventProductRetriever.getEventProductFromRedis(EVENT_PRODUCT_ID)).willReturn(productSnapshot(10));
			given(userRetriever.getUser(USER_ID))
					.willThrow(new UserNotFoundException());

		// when & then
		assertThatThrownBy(() -> checkoutService.getCheckout(EVENT_PRODUCT_ID, USER_ID))
				.isInstanceOf(BusinessException.class)
				.hasMessage("사용자를 찾을 수 없습니다.");
	}

	private EventProduct productSnapshot(final int totalStock) {
		return new EventProduct(
				EVENT_PRODUCT_ID,
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				totalStock,
				CHECK_IN_AT,
				CHECK_OUT_AT,
				OPEN_AT
		);
	}
}
