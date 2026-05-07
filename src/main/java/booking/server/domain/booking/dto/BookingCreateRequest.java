package booking.server.domain.booking.dto;

import booking.server.domain.booking.validation.ValidPaymentCombination;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@ValidPaymentCombination
public record BookingCreateRequest(
		@Schema(description = "예약할 이벤트 상품 ID", example = "1")
		@NotNull Long eventProductId,
		@Schema(description = "결제 수단별 결제 금액 목록. 결제 금액 합계는 상품 가격과 같아야 합니다.")
		@NotEmpty List<@Valid BookingPaymentRequest> payments
) {
}
