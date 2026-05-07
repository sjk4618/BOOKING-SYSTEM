package booking.server.domain.user.component;

import booking.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserPointManager {

	private final UserRepository userRepository;

	public boolean deduct(final long userId, final int amount) {
		return userRepository.deductPoint(userId, amount) > 0;
	}

	public void restore(final long userId, final int amount) {
		userRepository.restorePoint(userId, amount);
	}
}
