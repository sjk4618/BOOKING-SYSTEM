---
name: jpa-patterns
description: JPA/Hibernate patterns for entity design, relationships, query optimization, transactions, auditing, indexing, pagination, and pooling in Spring Boot with MySQL.
---

# JPA/Hibernate Patterns

Use for data modeling, repositories, and performance tuning in Spring Boot with MySQL.

## Entity Design

```java
@Entity
@Table(name = "markets", indexes = {
	@Index(name = "idx_markets_slug", columnList = "slug", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class MarketEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Enumerated(EnumType.STRING)
	private MarketStatus status = MarketStatus.ACTIVE;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;
}
```

Enable auditing:

```java
@Configuration
@EnableJpaAuditing
class JpaConfig {}
```

## Relationships and N+1 Prevention

```java
@OneToMany(mappedBy = "market", cascade = CascadeType.ALL, orphanRemoval = true)
private List<PositionEntity> positions = new ArrayList<>();
```

- Default to lazy loading.
- Avoid `EAGER` on collections.
- Use `JOIN FETCH`, `@EntityGraph`, or DTO projections for read paths.
- Check generated SQL when adding relationships.

```java
@Query("select m from MarketEntity m left join fetch m.positions where m.id = :id")
Optional<MarketEntity> findWithPositions(@Param("id") Long id);
```

## Repository Patterns

```java
public interface MarketRepository extends JpaRepository<MarketEntity, Long> {
	Optional<MarketEntity> findBySlug(String slug);

	@Query("select m from MarketEntity m where m.status = :status")
	Page<MarketEntity> findByStatus(@Param("status") MarketStatus status, Pageable pageable);
}
```

Use projections for lightweight queries:

```java
public interface MarketSummary {
	Long getId();
	String getName();
	MarketStatus getStatus();
}

Page<MarketSummary> findAllBy(Pageable pageable);
```

## Transactions

- Annotate service methods with `@Transactional`.
- Use `@Transactional(readOnly = true)` for read paths.
- Keep transactions short and avoid external API calls inside transactions.
- Choose propagation deliberately.

```java
@Transactional
public Market updateStatus(Long id, MarketStatus status) {
	MarketEntity entity = repo.findById(id)
		.orElseThrow(() -> new EntityNotFoundException("Market"));
	entity.setStatus(status);
	return Market.from(entity);
}
```

## Pagination

```java
PageRequest page = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
Page<MarketEntity> markets = repo.findByStatus(MarketStatus.ACTIVE, page);
```

For cursor-like pagination, use a stable sort key and a predicate such as `id > :lastId` or `(createdAt, id)` keyset conditions.

## Indexing and Performance

- Add indexes for common filters such as `status`, `slug`, and foreign keys.
- Use composite indexes that match query predicates and ordering, such as `(status, created_at)`.
- Avoid loading full entities for read-only list responses; project only needed columns.
- Batch writes with `saveAll` and `hibernate.jdbc.batch_size`.
- Validate query plans with MySQL `EXPLAIN`.

## Connection Pooling (HikariCP)

Recommended MySQL-oriented properties:

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.connection-test-query=SELECT 1
```

Recommended Hibernate JDBC settings for MySQL batch performance:

```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
```

Use the MySQL 8 dialect when an explicit dialect is needed:

```properties
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

## Caching

- The first-level cache is scoped to the current `EntityManager`.
- Do not keep managed entities across transactions.
- Consider second-level cache only for read-heavy data with a clear eviction strategy.

## Migrations

- Use Flyway or Liquibase.
- Do not rely on Hibernate auto DDL in production.
- Keep migrations additive where possible.
- Avoid dropping or rewriting columns without a rollback and data migration plan.

## Testing Data Access

- Prefer `@DataJpaTest` for repository tests.
- Use Testcontainers with MySQL when behavior depends on MySQL syntax, locking, indexes, or generated SQL.
- Assert SQL efficiency with logs:

```properties
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
```

Remember: keep entities lean, queries intentional, and transactions short. Prevent N+1 with fetch strategies and projections, and index for your read/write paths.
