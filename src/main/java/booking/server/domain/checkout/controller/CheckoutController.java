package booking.server.domain.checkout.controller;

import booking.server.domain.checkout.dto.CheckoutResponse;
import booking.server.domain.checkout.service.CheckoutService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@RequestMapping("/api/checkout")
public class CheckoutController implements CheckoutControllerDocs {

	private final CheckoutService checkoutService;

	@GetMapping
	@Override
	public ResponseEntity<CheckoutResponse> getCheckout(@RequestHeader("userId") final long userId,
														@RequestParam final long eventProductId) {
		return ResponseEntity
				.ok()
				.body(checkoutService.getCheckout(eventProductId, userId));
	}
}
