# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Spring Boot service built with Gradle. Application code lives under `src/main/java/booking/server`, with the entry point at `ServerApplication.java`. Domain code is grouped by business area under `domain`, including `booking`, `payment`, `stock`, `product`, `user`, and shared entity support in `common`. Runtime configuration belongs in `src/main/resources`, currently `application.yaml`. Tests mirror the main package structure under `src/test/java`. Design and workflow notes are kept in `docs/`, such as `docs/booking-payment-stock-flow.md`.

## Build, Test, and Development Commands

Use the Gradle wrapper so everyone runs the same Gradle version:

- `./gradlew compileJava`: compiles main Java sources and validates annotation processing.
- `./gradlew test`: runs the JUnit Platform test suite.
- `./gradlew build`: compiles, tests, and produces build artifacts under `build/`.
- `./gradlew bootRun`: starts the Spring Boot application locally.

On Windows, use `gradlew.bat` with the same task names.

## Coding Style & Naming Conventions

Follow the existing Java style: tabs for indentation in Java and Gradle files, lowercase package names, `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` enum constants. Keep domain entities named with the `Entity` suffix, such as `BookingEntity` or `PaymentEntity`. Place new domain concepts in the matching business package; add a package only for a distinct domain area. Lombok is available, but avoid hiding important lifecycle or validation logic.

## Testing Guidelines

Tests use Spring Boot Test and JUnit 5 via `useJUnitPlatform()`. Name test classes after the unit under test with a `Tests` suffix, for example `BookingServiceTests`. Implement the feature first when appropriate, then add focused tests for changed behavior before review. Prefer unit tests for domain and service logic; use `@SpringBootTest` only when the Spring context is part of what is being verified. Run `./gradlew test` before opening a PR.

## Commit

This repository currently has no commit history to infer a project-specific convention. Use short, imperative commit messages, optionally scoped: `Add booking stock reservation entity` or `Fix payment status transition`. Keep each commit focused on one behavior or refactor.

## Security & Configuration Tips

Do not commit secrets, database passwords, or local credentials. Keep environment-specific settings outside tracked files or inject them through environment variables and Spring profiles. Document any required local MySQL setup in `docs/` when adding database-dependent behavior.

## Agent-Specific Instructions

Role-specific agent prompts are kept in `.ai/agents/`, and reusable skills belong in `.ai/skills/`. See `.ai/README.md` for recommended agent-skill pairings and usage examples.

## Local AI Commands

When the user types `/code-review`, read `.ai/commands/code-review.md` and follow it. When the user types `/plan`, read `.ai/commands/plan.md` and follow it. When the user types `/write-tests`, read `.ai/commands/write-tests.md` and follow it.
