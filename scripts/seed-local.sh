#!/usr/bin/env bash
set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-booking-mysql}"
REDIS_CONTAINER="${REDIS_CONTAINER:-booking-redis}"
MYSQL_DATABASE="${MYSQL_DATABASE:-booking_server}"
MYSQL_USER="${MYSQL_USER:-booking}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-booking}"

echo "Seeding MySQL demo data..."
docker exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE}" <<'SQL'
DELETE FROM payments;
DELETE FROM stock_histories;
DELETE FROM bookings;
DELETE FROM idempotency_keys;

INSERT INTO users (id, name, available_point, created_at, updated_at)
VALUES
	(1, 'Postman User', 100000, NOW(), NOW()),
	(2, 'Second User', 100000, NOW(), NOW())
ON DUPLICATE KEY UPDATE
	name = VALUES(name),
	available_point = VALUES(available_point),
	updated_at = NOW();

INSERT INTO event_products
	(id, name, price, total_stock, used_stock, check_in, check_out, open_at, created_at, updated_at)
VALUES
	(1, 'Postman Hotel Package', 50000, 10, 0, '2026-06-01 15:00:00', '2026-06-02 11:00:00', '2026-05-10 00:00:00', NOW(), NOW()),
	(2, 'Small Stock Package', 30000, 1, 0, '2026-06-03 15:00:00', '2026-06-04 11:00:00', '2026-05-10 00:00:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE
	name = VALUES(name),
	price = VALUES(price),
	total_stock = VALUES(total_stock),
	used_stock = VALUES(used_stock),
	check_in = VALUES(check_in),
	check_out = VALUES(check_out),
	open_at = VALUES(open_at),
	updated_at = NOW();
SQL

echo "Seeding Redis demo cache and stock keys..."
docker exec -i "${REDIS_CONTAINER}" redis-cli <<'REDIS'
DEL event-product:1:stock:used event-product:1:stock:users event-product:1:stock:sold-out
DEL event-product:2:stock:used event-product:2:stock:users event-product:2:stock:sold-out
SETEX checkout:event-product:1 600 "{\"eventProductId\":1,\"name\":\"Postman Hotel Package\",\"price\":50000,\"totalStock\":10,\"checkInAt\":\"2026-06-01T15:00:00\",\"checkOutAt\":\"2026-06-02T11:00:00\",\"openAt\":\"2026-05-10T00:00:00\"}"
SETEX checkout:event-product:2 600 "{\"eventProductId\":2,\"name\":\"Small Stock Package\",\"price\":30000,\"totalStock\":1,\"checkInAt\":\"2026-06-03T15:00:00\",\"checkOutAt\":\"2026-06-04T11:00:00\",\"openAt\":\"2026-05-10T00:00:00\"}"
REDIS

echo "Done."
echo "Postman defaults: userId=1, eventProductId=1, price=50000"
