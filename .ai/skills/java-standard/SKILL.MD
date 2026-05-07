---
name: java-coding-standards
description: Java coding standards for Spring Boot services( naming, immutability, Optional usage, streams, exceptions, generics, and project layout.)
---

# Java Coding Standards

Standards for readable, maintainable Java 17+ code in Spring Boot services.

## Core Principles

- Prefer clarity over cleverness.
- Immutable by default; minimize shared mutable state.
- Fail fast with meaningful exceptions.
- Use consistent naming and package structure.

## Naming

```java
// Classes/Records: PascalCase
public class MarketService {}
public record Money(BigDecimal amount, Currency currency) {}

// Methods/fields: camelCase
private final MarketRepository marketRepository;
public Market findBySlug(String slug) {}

// Constants: UPPER_SNAKE_CASE
private static final int MAX_PAGE_SIZE = 100;
```

## Immutability

```java
// Favor records and final fields
public record MarketDto(Long id, String name, MarketStatus status) {}

public class Market {
	private final Long id;
	private final String name;

	public Market(Long id, String name) {
		this.id = id;
		this.name = name;
	}
}
```

## Optional Usage

```java
// Return Optional from find* methods
Optional<Market> market = marketRepository.findBySlug(slug);

// Use map/flatMap instead of get()
return market
	.map(MarketResponse::from)
	.orElseThrow(() -> new EntityNotFoundException("Market not found"));
```

## Streams Best Practices

```java
// Use streams for transformations, keep pipelines short
List<String> names = markets.stream()
	.map(Market::name)
	.filter(Objects::nonNull)
	.toList();
```

Avoid complex nested streams. Prefer loops when they are clearer.

## Exceptions

- Use unchecked exceptions for domain errors.
- Wrap technical exceptions with useful context.
- Create domain-specific exceptions, such as `MarketNotFoundException`.
- Avoid broad `catch (Exception ex)` unless rethrowing or logging centrally.

```java
throw new MarketNotFoundException(slug);
```

## Generics and Type Safety

- Avoid raw types; declare generic parameters.
- Prefer bounded generics for reusable utilities.

```java
public <T extends Identifiable> Map<Long, T> indexById(Collection<T> items) {
	return items.stream().collect(Collectors.toMap(T::id, Function.identity()));
}
```

## Project Structure

```text
src/main/java/booking/server/
  config/
  controller/
  service/
  repository/
  domain/
  dto/
  util/
src/main/resources/
  application.yaml
src/test/java/booking/server/
```

Mirror main packages in `src/test/java`. Keep this repository's domain-first packages when extending existing domain code.

## Formatting and Style

- Use the repository's existing Java formatting consistently.
- Use one public top-level type per file.
- Keep methods short and focused; extract helpers when behavior grows.
- Order members as constants, fields, constructors, public methods, protected methods, then private methods.

## Code Smells to Avoid

- Long parameter lists; use DTOs or builders.
- Deep nesting; prefer early returns.
- Magic numbers; use named constants.
- Static mutable state; prefer dependency injection.
- Silent catch blocks; log and act or rethrow.

## Logging

```java
private static final Logger log = LoggerFactory.getLogger(MarketService.class);

log.info("fetch_market slug={}", slug);
log.error("failed_fetch_market slug={}", slug, ex);
```

## Null Handling

- Accept `@Nullable` only when unavoidable.
- Prefer explicit validation for required input.
- Use Bean Validation annotations such as `@NotNull` and `@NotBlank` on request inputs.

## Testing Expectations

- Use JUnit 5 for tests.
- Use AssertJ for fluent assertions when available.
- Use Mockito for mocking; avoid partial mocks where possible.
- Favor deterministic tests; avoid hidden sleeps.

Remember: keep code intentional, typed, and observable. Optimize for maintainability over micro-optimizations unless proven necessary.
