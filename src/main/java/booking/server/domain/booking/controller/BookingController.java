package booking.server.domain.booking.controller;

import booking.server.domain.booking.dto.BookingCreateResult;
import booking.server.domain.booking.dto.BookingCreateRequest;
import booking.server.domain.booking.service.BookingFacade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/bookings")
public class BookingController {

	private final BookingFacade bookingFacade;

	@PostMapping
	public ResponseEntity<Object> createBooking(@Positive @RequestHeader(value = "userId", required = true) final long userId,
												@NotBlank @RequestHeader(value = "Idempotency-Key", required = true) final String idempotencyKey,
												@Valid @RequestBody final BookingCreateRequest request) {
		BookingCreateResult result = bookingFacade.createBooking(userId, idempotencyKey, request);
		return ResponseEntity
				.status(result.httpStatus())
				.body(result.body());
	}
}
