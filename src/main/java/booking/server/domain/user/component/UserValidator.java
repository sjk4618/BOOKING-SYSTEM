package booking.server.domain.user.component;

import booking.server.domain.user.exception.UserNotFoundException;
import booking.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {

	private final UserRepository userRepository;

	public void validateExists(final long userId) {
		if (!userRepository.existsById(userId)) {
			throw new UserNotFoundException();
		}
	}
}
