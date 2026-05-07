package booking.server.domain.checkout.service;

import booking.server.domain.checkout.component.StockUsageRetriever;
import booking.server.domain.checkout.component.StockUsageResult;
import booking.server.domain.checkout.component.UserRetriever;
import booking.server.domain.checkout.dto.CheckoutResponse;
import booking.server.domain.checkout.dto.CheckoutUserResponse;
import booking.server.domain.eventproduct.component.EventProductRetriever;
import booking.server.domain.eventproduct.domain.EventProduct;
import booking.server.domain.eventproduct.exception.EventProductNotFoundException;
import booking.server.domain.user.domain.entity.User;
import booking.server.domain.user.exception.UserNotFoundException;
import booking.server.global.exception.BusinessException;
import booking.server.global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckoutService {

	private final EventProductRetriever eventProductRetriever;
	private final UserRetriever userRetriever;
	private final StockUsageRetriever stockUsageRetriever;

	@Transactional(readOnly = true)
	public CheckoutResponse getCheckout(final long eventProductId, final long userId) {
		final EventProduct eventProduct = getEventProduct(eventProductId);
		final User user = getUser(userId);
		validateStockAvailable(eventProduct.eventProductId(), eventProduct.totalStock());

		return new CheckoutResponse(
				eventProduct.eventProductId(),
				eventProduct.name(),
				eventProduct.price(),
				eventProduct.checkInAt(),
				eventProduct.checkOutAt(),
				eventProduct.openAt(),
				new CheckoutUserResponse(userId, user.getName(), user.getAvailablePoint())
		);
	}

	private EventProduct getEventProduct(final long eventProductId) {
		try {
			return eventProductRetriever.getEventProductFromRedis(eventProductId);
		} catch (EventProductNotFoundException e) {
			throw new BusinessException(ErrorCode.EVENT_PRODUCT_NOT_FOUND);
		}
	}

	private User getUser(final long userId) {
		try {
			return userRetriever.getUser(userId);
		} catch (UserNotFoundException e) {
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);
		}
	}

	private void validateStockAvailable(final long eventProductId, final int eventProductTotalStock) {
		final StockUsageResult stockUsage = stockUsageRetriever.getStockUsage(eventProductId);
		if (!stockUsage.redisAvailable()) {
			return;
		}

		final int availableQuantity = eventProductTotalStock - Math.toIntExact(stockUsage.usedCount());
		if (availableQuantity <= 0) {
			throw new BusinessException(ErrorCode.SOLD_OUT);
		}
	}
}
