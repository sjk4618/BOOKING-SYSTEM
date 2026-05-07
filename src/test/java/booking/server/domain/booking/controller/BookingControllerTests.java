package booking.server.domain.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import booking.server.domain.booking.domain.entity.BookingStatus;
import booking.server.domain.booking.dto.BookingCreateResult;
import booking.server.domain.booking.dto.BookingPaymentResponse;
import booking.server.domain.booking.dto.BookingResponse;
import booking.server.domain.booking.service.BookingFacade;
import booking.server.domain.payment.domain.entity.PaymentMethod;
import booking.server.domain.payment.domain.entity.PaymentStatus;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import booking.server.global.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BookingFacade bookingFacade;

	@Test
	@DisplayName("예약 생성에 성공하면 201과 확정 예약 응답을 반환한다")
	void createBooking_성공_201반환() throws Exception {
		// given
		given(bookingFacade.createBooking(eq(10L), eq("key-1"), any()))
				.willReturn(new BookingCreateResult(HttpStatus.CREATED.value(), bookingResponse()));

		// when & then
		mockMvc.perform(post("/api/bookings")
						.header("userId", "10")
						.header("Idempotency-Key", "key-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "eventProductId": 1,
								  "payments": [
								    {"paymentMethod": "CREDIT_CARD", "amount": 100000}
								  ]
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.bookingId").value(100))
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.payments[0].status").value("COMPLETED"));
	}

	@Test
	@DisplayName("신용카드와 Y페이를 함께 요청하면 본문 검증에 실패한다")
	void createBooking_신용카드와Y페이혼용_400반환() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.header("userId", "10")
						.header("Idempotency-Key", "key-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "eventProductId": 1,
								  "payments": [
								    {"paymentMethod": "CREDIT_CARD", "amount": 50000},
								    {"paymentMethod": "Y_PAY", "amount": 50000}
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(40000));
	}

	@Test
	@DisplayName("필수 헤더가 없으면 400을 반환한다")
	void createBooking_필수헤더누락_400반환() throws Exception {
		// when & then
		mockMvc.perform(post("/api/bookings")
						.header("userId", "10")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "eventProductId": 1,
								  "payments": [
								    {"paymentMethod": "CREDIT_CARD", "amount": 100000}
								  ]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(40000));
	}

	@Test
	@DisplayName("요청 본문 검증에 실패하면 400을 반환한다")
	void createBooking_본문검증실패_400반환() throws Exception {
		// when & then
		mockMvc.perform(post("/api/bookings")
						.header("userId", "10")
						.header("Idempotency-Key", "key-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "eventProductId": 1,
								  "payments": []
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(40000));
	}

	@Test
	@DisplayName("같은 멱등키로 다른 요청이 들어오면 422를 반환한다")
	void createBooking_멱등키요청불일치_422반환() throws Exception {
		// given
		given(bookingFacade.createBooking(eq(10L), eq("key-1"), any()))
				.willThrow(new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_MISMATCH));

		// when & then
		mockMvc.perform(post("/api/bookings")
						.header("userId", "10")
						.header("Idempotency-Key", "key-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "eventProductId": 1,
								  "payments": [
								    {"paymentMethod": "CREDIT_CARD", "amount": 100000}
								  ]
								}
								"""))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value(42201));
	}

	private BookingResponse bookingResponse() {
		return new BookingResponse(
				100L,
				1L,
				10L,
				BookingStatus.CONFIRMED,
				BigDecimal.valueOf(100_000),
				LocalDateTime.of(2026, 5, 7, 10, 3),
				List.of(new BookingPaymentResponse(
						1L,
						PaymentMethod.CREDIT_CARD,
						BigDecimal.valueOf(100_000),
						PaymentStatus.COMPLETED
				))
		);
	}
}
