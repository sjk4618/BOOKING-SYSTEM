package booking.server.domain.checkout.controller;

import booking.server.domain.checkout.dto.CheckoutResponse;
import booking.server.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Checkout", description = "예약 전 상품과 사용자 결제 정보를 조회합니다.")
public interface CheckoutControllerDocs {

	@Operation(
			summary = "Checkout 조회",
			description = "상품 정보, 체크인/체크아웃 시간, 사용자 보유 포인트를 조회합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "조회 성공",
					content = @Content(schema = @Schema(implementation = CheckoutResponse.class))
			),
			@ApiResponse(
					responseCode = "404",
					description = "상품 또는 사용자를 찾을 수 없음",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			),
			@ApiResponse(
					responseCode = "409",
					description = "예약 가능한 재고 없음",
					content = @Content(schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	ResponseEntity<CheckoutResponse> getCheckout(
			@Parameter(description = "사용자 ID", example = "1", required = true)
			@RequestHeader("userId") long userId,
			@Parameter(description = "이벤트 상품 ID", example = "1", required = true)
			@RequestParam long eventProductId
	);
}
