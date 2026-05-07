package booking.server.domain.booking.controller;

import booking.server.domain.booking.dto.BookingCreateRequest;
import booking.server.domain.booking.dto.BookingResponse;
import booking.server.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Booking", description = "한정 수량 이벤트 상품을 예약하고 결제를 완료합니다.")
public interface BookingControllerDocs {

	@Operation(
			summary = "예약 생성",
			description = "Redis로 재고를 선점한 뒤 결제와 예약 확정을 처리합니다. 같은 Idempotency-Key로 같은 요청을 다시 보내면 저장된 응답을 재사용합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "201",
					description = "예약 생성 성공",
					content = @Content(schema = @Schema(implementation = BookingResponse.class))
			),
			@ApiResponse(
					responseCode = "400",
					description = "요청 형식 또는 결제 금액 오류",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "404",
					description = "상품 또는 사용자를 찾을 수 없음",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "409",
					description = "품절, 중복 예약, 결제 실패, 멱등키 처리 중 등 비즈니스 충돌",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "422",
					description = "같은 멱등키로 다른 요청을 보냄",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	ResponseEntity<Object> createBooking(
			@Parameter(description = "사용자 ID", example = "1", required = true)
			@Positive @RequestHeader(value = "userId", required = true) long userId,
			@Parameter(description = "멱등키. 요청마다 새 값을 사용하세요.", example = "postman-001", required = true)
			@NotBlank @RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey,
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "예약 생성 요청. 결제 금액 합계는 상품 가격과 같아야 합니다.",
					content = @Content(
							schema = @Schema(implementation = BookingCreateRequest.class),
							examples = @ExampleObject(
									name = "신용카드 결제",
									value = """
											{
											  "eventProductId": 1,
											  "payments": [
											    {
											      "paymentMethod": "CREDIT_CARD",
											      "amount": 50000
											    }
											  ]
											}
											"""
							)
					)
			)
			@Valid @RequestBody BookingCreateRequest request
	);
}
