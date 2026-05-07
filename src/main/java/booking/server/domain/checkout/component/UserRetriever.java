package booking.server.domain.checkout.component;

import booking.server.domain.user.domain.entity.User;
import booking.server.domain.user.exception.UserNotFoundException;
import booking.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRetriever {

	private final UserRepository userRepository;

	public User getUser(final long userId) {
		return userRepository.findById(userId)
				.orElseThrow(UserNotFoundException::new);
	}
}
