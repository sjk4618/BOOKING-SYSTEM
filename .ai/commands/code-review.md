# /code-review

Use `.ai/agents/code-reviewer.md`.

## Purpose

Review the latest code changes for quality, security, maintainability, test coverage, and performance.

## Required Context

- Run `git diff` first.
- Focus on modified files.
- Load related skills based on changed files:
  - `.ai/skills/java-standard.md`
  - `.ai/skills/springboot-standard.md`
  - `.ai/skills/jpa-standard.md`
  - `.ai/skills/test-guide.md`

## Command Prompt

```text
Use .ai/agents/code-reviewer.md to review the latest git diff.
Apply relevant skills from .ai/skills/: java-standard, springboot-standard, jpa-standard, and test-guide.
Prioritize CRITICAL and HIGH issues first.
Return findings by severity with file paths, issues, and concrete fixes.
```
