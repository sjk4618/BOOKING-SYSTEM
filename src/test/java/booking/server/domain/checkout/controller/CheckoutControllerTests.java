package booking.server.domain.checkout.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import booking.server.domain.checkout.dto.CheckoutResponse;
import booking.server.domain.checkout.dto.CheckoutUserResponse;
import booking.server.domain.checkout.service.CheckoutService;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import booking.server.global.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckoutController.class)
@Import(GlobalExceptionHandler.class)
class CheckoutControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CheckoutService checkoutService;

	@Test
	@DisplayName("checkout 조회에 성공하면 상품, 사용자, 재고 정보를 반환한다")
	void getCheckout_성공_checkout정보반환() throws Exception {
		// given
		given(checkoutService.getCheckout(1L, 10L)).willReturn(checkoutResponse());

		// when & then
		mockMvc.perform(get("/api/checkout")
						.header("userId", "10")
						.param("eventProductId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventProductId").value(1))
				.andExpect(jsonPath("$.name").value("Hotel Package A"))
				.andExpect(jsonPath("$.price").value(100000))
				.andExpect(jsonPath("$.user.userId").value(10))
				.andExpect(jsonPath("$.user.availablePoint").value(5000));
	}

	@Test
	@DisplayName("품절이면 409 OUT_OF_STOCK을 반환한다")
	void getCheckout_품절_409반환() throws Exception {
		// given
		given(checkoutService.getCheckout(1L, 10L))
				.willThrow(new BusinessException(ErrorCode.SOLD_OUT));

		// when & then
		mockMvc.perform(get("/api/checkout")
				.header("userId", "10")
				.param("eventProductId", "1"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(40901))
				.andExpect(jsonPath("$.message").value("현재 예약 가능한 재고가 없습니다."));
	}

	@Test
	@DisplayName("상품이나 사용자가 없으면 404를 반환한다")
	void getCheckout_조회대상없음_404반환() throws Exception {
		// given
		given(checkoutService.getCheckout(1L, 10L))
				.willThrow(new BusinessException(ErrorCode.EVENT_PRODUCT_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/checkout")
				.header("userId", "10")
				.param("eventProductId", "1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value(40401))
				.andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("필수 파라미터가 없으면 400을 반환한다")
	void getCheckout_파라미터누락_400반환() throws Exception {
		// when & then
		mockMvc.perform(get("/api/checkout")
						.param("eventProductId", "1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(40000))
				.andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
	}

	private CheckoutResponse checkoutResponse() {
		return new CheckoutResponse(
				1L,
				"Hotel Package A",
				BigDecimal.valueOf(100_000),
				LocalDateTime.of(2026, 6, 1, 15, 0),
				LocalDateTime.of(2026, 6, 2, 11, 0),
				LocalDateTime.of(2026, 5, 10, 10, 0),
				new CheckoutUserResponse(10L, "user", 5_000)
		);
	}
}
