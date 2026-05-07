package booking.server.domain.idempotency.component;

import booking.server.domain.booking.dto.BookingResponse;
import booking.server.global.exception.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyResponseSerializer {

	private final ObjectMapper objectMapper;

	public String serialize(final Object response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize idempotency response", exception);
		}
	}

	public Object deserialize(final int httpStatus, final String responseBody) {
		try {
			if (HttpStatus.valueOf(httpStatus).is2xxSuccessful()) {
				return objectMapper.readValue(responseBody, BookingResponse.class);
			}
			return objectMapper.readValue(responseBody, ErrorResponse.class);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize idempotency response", exception);
		}
	}
}
