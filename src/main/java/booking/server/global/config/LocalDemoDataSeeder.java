package booking.server.global.config;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoDataSeeder {

	private static final Duration CACHE_TTL = Duration.ofMinutes(10);

	private final JdbcTemplate jdbcTemplate;
	private final StringRedisTemplate redisTemplate;

	@EventListener(ApplicationReadyEvent.class)
	public void seed() {
		seedMysql();
		seedRedis();
	}

	private void seedMysql() {
		jdbcTemplate.update("""
				delete from payments
				where booking_id in (
					select id from bookings
					where event_product_id in (1, 2) or user_id in (1, 2)
				)
				""");
		jdbcTemplate.update("delete from stock_histories where event_product_id in (1, 2) or user_id in (1, 2)");
		jdbcTemplate.update("delete from bookings where event_product_id in (1, 2) or user_id in (1, 2)");
		jdbcTemplate.update("delete from idempotency_keys");
		jdbcTemplate.update("""
				insert into users (id, name, available_point, created_at, updated_at)
				values
					(1, 'Postman User', 100000, now(), now()),
					(2, 'Second User', 100000, now(), now())
				on duplicate key update
					name = values(name),
					available_point = values(available_point),
					updated_at = now()
				""");
		jdbcTemplate.update("""
				insert into event_products
					(id, name, price, total_stock, used_stock, check_in, check_out, open_at, created_at, updated_at)
				values
					(1, 'Postman Hotel Package', 50000, 10, 0, '2026-06-01 15:00:00', '2026-06-02 11:00:00', '2026-05-10 00:00:00', now(), now()),
					(2, 'Small Stock Package', 30000, 1, 0, '2026-06-03 15:00:00', '2026-06-04 11:00:00', '2026-05-10 00:00:00', now(), now())
				on duplicate key update
					name = values(name),
					price = values(price),
					total_stock = values(total_stock),
					used_stock = values(used_stock),
					check_in = values(check_in),
					check_out = values(check_out),
					open_at = values(open_at),
					updated_at = now()
				""");
	}

	private void seedRedis() {
		redisTemplate.delete(List.of(
				"event-product:1:stock:used",
				"event-product:1:stock:users",
				"event-product:1:stock:sold-out",
				"event-product:2:stock:used",
				"event-product:2:stock:users",
				"event-product:2:stock:sold-out"
		));
		redisTemplate.opsForValue().set(
				"checkout:event-product:1",
				"""
						{"eventProductId":1,"name":"Postman Hotel Package","price":50000,"totalStock":10,"checkInAt":"2026-06-01T15:00:00","checkOutAt":"2026-06-02T11:00:00","openAt":"2026-05-10T00:00:00"}
						""".trim(),
				CACHE_TTL
		);
		redisTemplate.opsForValue().set(
				"checkout:event-product:2",
				"""
						{"eventProductId":2,"name":"Small Stock Package","price":30000,"totalStock":1,"checkInAt":"2026-06-03T15:00:00","checkOutAt":"2026-06-04T11:00:00","openAt":"2026-05-10T00:00:00"}
						""".trim(),
				CACHE_TTL
		);
	}
}
