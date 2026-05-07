# /write-tests

Use `.ai/agents/test-writer.md`.

## Purpose

Write focused Spring Boot test code after feature implementation.

## Required Context

- Inspect changed files and implemented behavior.
- Identify core business rules, edge cases, failure paths, and persistence behavior.
- Load related skills:
  - `.ai/skills/test-guide.md`
  - `.ai/skills/java-standard.md`
  - `.ai/skills/springboot-standard.md`
  - `.ai/skills/jpa-standard.md`

## Command Prompt

```text
Use .ai/agents/test-writer.md to write focused tests for the implemented changes.
Apply test-guide and any relevant Java, Spring Boot, or JPA skills.
Create or update test files under src/test/java following the main package structure.
Run the appropriate Gradle test command, generate/check JaCoCo coverage, always show the user the latest JaCoCo coverage numbers, and report remaining test gaps.
```
