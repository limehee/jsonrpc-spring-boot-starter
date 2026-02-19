# Contributing Guide

Thanks for contributing to `jsonrpc-spring-boot-starter`.

## Scope

This project provides JSON-RPC 2.0 server components for Spring Boot:
- core protocol/dispatch module
- Spring WebMVC transport module
- Spring Boot auto-configuration and starter

Protocol behavior should stay aligned with the JSON-RPC 2.0 specification.

## Before You Start

1. Check existing issues and pull requests to avoid duplicate work.
2. For non-trivial changes, open an issue first and discuss the approach.
3. Keep changes focused and small when possible.

## Development Setup

Requirements:
- JDK 17+
- Gradle wrapper (`./gradlew`)

Common commands:

```bash
./gradlew test
./gradlew check
./scripts/verify-consumer-smoke.sh
./gradlew -p samples/spring-boot-demo classes
```

## Coding Guidelines

- Follow existing module boundaries and abstraction style.
- Preserve JSON-RPC 2.0 compliance.
- Add or update tests for:
  - success paths
  - failure paths
  - exception/edge branches
- Keep public API behavior backward compatible unless a breaking change is intentional and documented.

## Commit and PR Guidelines

- Write clear commit messages describing intent.
- Update docs when behavior/configuration changes.
- Add clear release notes in the GitHub Release for user-visible changes.

Before opening a PR, make sure:

1. Tests pass locally.
2. New behavior is covered by tests.
3. Documentation is updated.
4. The PR description explains motivation, approach, and trade-offs.

## Reporting Security Issues

Do not open public issues for sensitive security vulnerabilities.
Share details privately with maintainers first.
See `SECURITY.md` for policy details.
