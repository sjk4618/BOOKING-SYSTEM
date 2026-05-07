package booking.server.domain.checkout.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import booking.server.domain.user.domain.entity.User;
import booking.server.domain.user.exception.UserNotFoundException;
import booking.server.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRetrieverTests {

	private static final Long USER_ID = 10L;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserRetriever userRetriever;

	@Test
	@DisplayName("사용자를 조회한다")
	void read_사용자있음_사용자반환() {
		// given
		User user = User.create("user", 5_000);
		given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

		// when
		User result = userRetriever.getUser(USER_ID);

		// then
		assertThat(result).isEqualTo(user);
	}

	@Test
	@DisplayName("사용자를 찾을 수 없으면 예외가 발생한다")
	void read_사용자없음_예외() {
		// given
		given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userRetriever.getUser(USER_ID))
				.isInstanceOf(UserNotFoundException.class);
	}
}
