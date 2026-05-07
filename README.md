# Booking Server

## Local Infrastructure

Docker만 설치되어 있으면 MySQL과 Redis를 함께 실행할 수 있다.

```bash
docker compose up -d
```

기본 접속 정보:

```text
MySQL host: localhost
MySQL port: 13306
Database: booking_server
Username: booking
Password: booking

Redis host: localhost
Redis port: 16379
```

애플리케이션은 `local` 프로필로 실행한다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

테스트는 H2 인메모리 DB를 사용하므로 Docker 없이 실행할 수 있다.

```bash
./gradlew test
```

인프라를 중지하려면 다음 명령을 사용한다.

```bash
docker compose down
```

데이터까지 모두 삭제해야 할 때만 볼륨을 함께 제거한다.

```bash
docker compose down -v
```
