package booking.server.domain.booking.dto;

import booking.server.domain.booking.validation.ValidPaymentCombination;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@ValidPaymentCombination
public record BookingCreateRequest(
		@NotNull Long eventProductId,
		@NotEmpty List<@Valid BookingPaymentRequest> payments
) {
}
