package booking.server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI bookingOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Booking Server API")
						.description("Checkout 조회와 한정 수량 상품 예약 생성 API")
						.version("v1"));
	}
}
