---
name: springboot-test-guide
description: Post-implementation testing guide for Spring Boot using JUnit 5, Mockito, MockMvc, DataJpaTest, Testcontainers, and Gradle.
---

# Spring Boot Testing Guide

Use this guide after or during feature implementation to verify behavior. Write implementation first when appropriate, then add focused tests before review or merge.

## When to Use

- After implementing a new feature or endpoint
- After fixing a bug
- After changing domain rules, transactions, repositories, or validation
- Before code review for behavior-changing code

## Test Scope Selection

- Use unit tests for domain rules, validators, mappers, and service decisions.
- Use `@WebMvcTest` for controller request/response, validation, and exception mapping.
- Use `@DataJpaTest` for repository queries, entity mapping, indexes, and persistence behavior.
- Use `@SpringBootTest` only when multiple Spring layers must be verified together.
- Use Testcontainers with MySQL when behavior depends on SQL dialect, locking, indexes, or real database constraints.

## Unit Tests (JUnit 5 + Mockito)

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTests {
	@Mock BookingRepository bookingRepository;
	@InjectMocks BookingService bookingService;

	@Test
	void createsBookingWhenStockIsAvailable() {
		when(bookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Booking result = bookingService.create(command);

		assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
		verify(bookingRepository).save(any());
	}
}
```

Patterns:

- Use Arrange-Act-Assert.
- Test behavior, not private methods.
- Prefer explicit stubbing over partial mocks.
- Use `@ParameterizedTest` for variants and boundaries.

## Web Layer Tests (MockMvc)

```java
@WebMvcTest(BookingController.class)
class BookingControllerTests {
	@Autowired MockMvc mockMvc;
	@MockBean BookingService bookingService;

	@Test
	void returnsBadRequestForInvalidRequest() throws Exception {
		mockMvc.perform(post("/api/bookings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isBadRequest());
	}
}
```

Verify status codes, response body shape, validation errors, and exception handling.

## Persistence Tests (DataJpaTest)

```java
@DataJpaTest
class BookingRepositoryTests {
	@Autowired BookingRepository bookingRepository;

	@Test
	void findsBookingsByStatus() {
		bookingRepository.save(bookingEntity);

		List<BookingEntity> results = bookingRepository.findByStatus(BookingStatus.CONFIRMED);

		assertThat(results).hasSize(1);
	}
}
```

Use Testcontainers MySQL for MySQL-specific behavior. Keep repository tests focused on queries and mapping.

## Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingIntegrationTests {
	@Autowired MockMvc mockMvc;

	@Test
	void createsBooking() throws Exception {
		mockMvc.perform(post("/api/bookings")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"productId":1,"userId":1,"quantity":2}
				"""))
			.andExpect(status().isCreated());
	}
}
```

Use integration tests for critical flows where controller, service, transaction, and persistence behavior must work together.

## Edge Cases to Cover

- Null, empty, invalid, duplicate, and boundary values
- Not found, forbidden, conflict, and validation failures
- Database constraint violations
- Transaction rollback paths
- Concurrent access for stock, booking, payment, and idempotency flows
- External dependency failures when integrations exist

## Test Quality Checklist

- Test names describe the behavior.
- Assertions are specific and meaningful.
- Tests are deterministic and independent.
- Setup data is local to the test or clearly isolated.
- Error paths are covered, not only happy paths.
- No hidden sleeps or timing-dependent assertions.
- New behavior has tests, or the lack of tests is explicitly justified.

## Commands

```bash
./gradlew test
./gradlew test jacocoTestReport
./gradlew jacocoTestCoverageVerification
./gradlew build
```

Use `./gradlew test` for normal verification, and always generate/check JaCoCo coverage with `./gradlew test jacocoTestReport` when tests are part of the task. Use `./gradlew jacocoTestCoverageVerification` when coverage rules are configured. Use `./gradlew build` before merge when compile, tests, and packaging all need validation.

## Coverage Reporting

- Always inspect JaCoCo after running tests.
- Report the HTML and XML report paths when available.
- Summarize the main coverage counters, especially line and branch coverage.
- If coverage is low, identify the untested behavior rather than chasing trivial getter or boilerplate coverage.
- Default report paths:
  - HTML: `build/reports/jacoco/test/html/index.html`
  - XML: `build/reports/jacoco/test/jacocoTestReport.xml`

Remember: implementation may come first, but behavior-changing code should not be considered complete until meaningful tests pass.
