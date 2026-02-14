# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

### Added
- Modular JSON-RPC core pipeline (parser, validator, registry, invoker, exception resolver, response composer).
- Dedicated `jsonrpc-spring-webmvc` transport module.
- Typed handler adapters and annotation-driven registration (`@JsonRpcMethod`, `@JsonRpcParam`).
- Interceptor chain extension points and Micrometer metrics interceptor.
- Method access controls (allowlist/denylist) and registration conflict policy.
- Optional notification executor strategy integration.
- Spring configuration metadata for `jsonrpc.*` properties.
- GitHub Actions workflows for CI and publish.
- Expanded branch-focused tests for parser/validator/binder, dispatcher/interceptor errors, HTTP endpoint/status strategy, and autoconfiguration access-control behavior.
- Runnable sample module at `samples/spring-boot-demo` with annotation-based JSON-RPC methods and curl examples.
- Contribution assets: `CONTRIBUTING.md`, GitHub issue templates, and pull request template.
- Integration tests for `samples/spring-boot-demo` covering bean registration, method/param binding, JSON response shape, and end-to-end invocation.
- Multi-stage test structure support in library modules: `test` (unit), `integrationTest`, and `e2eTest` Gradle tasks.
- Library-level integration and end-to-end tests for auto-configuration/HTTP execution paths.
- Added library integration/e2e scenarios that validate all primary registration styles (`@JsonRpcMethod`, explicit `JsonRpcMethodRegistration`, and `JsonRpcTypedMethodHandlerFactory`) with class/record/collection parameter and return payloads.
- Added pure Java (no Spring) integration/e2e tests in `jsonrpc-core` and standalone usage documentation in README.
- Expanded metrics coverage with transport/batch/notification instrumentation and configurable latency histogram/percentiles/method-tag cardinality limits.
- Added `SECURITY.md` with vulnerability reporting policy.
- Added consumer smoke automation (`scripts/verify-consumer-smoke.sh` and `.github/workflows/consumer-smoke.yml`) to verify published artifacts from fresh Maven/Gradle consumer projects.

### Changed
- Documentation references were refreshed for current CI matrix (Java 17/21/25), consumer smoke verification, and updated JMH coverage.
- Rewrote `README.md` as a documentation entrypoint and expanded official-style documentation under `docs/` (getting started, architecture, protocol compliance, configuration reference, extension points, testing, performance, troubleshooting).
- WebMVC endpoint now emits observer callbacks for parse errors, payload-size violations, batch aggregation, and notification-only calls.
- Reduced runtime metrics overhead by caching Micrometer counters and replacing stack-trace scans with typed failure markers for access-control/interceptor paths.
- Expanded JMH dispatcher benchmark scenarios to cover success, invalid request/params, method-not-found, and large batch profiles.
- Added `:jsonrpc-core:jmhQuick` task for short JMH smoke profiling, with optional include filter (`-PjmhQuickInclude=...`).
- Publishing metadata now uses resolved dependency versions (`versionMapping`) so Gradle/Maven consumers can resolve released artifacts without missing-version failures.
- JSON-RPC compliance handling for batch, notification, and invalid request edge cases.
- HTTP endpoint behavior: JSON content-type enforcement and request size limit handling.
- Build now uses Gradle Version Catalog (`gradle/libs.versions.toml`).
- Core API nullability contracts now use JSpecify annotations.
- GitHub Actions updated to latest major/patch releases and Gradle configuration cache enabled in CI/publish workflows.
- Strict JSON-RPC alignment: reserved `rpc.*` methods are always rejected and parse-only-whitespace payloads now return `Parse error` (-32700).
- README reorganized for better onboarding with basic vs advanced usage sections.
- Spring configuration metadata now clarifies that `rpc.*` methods are always reserved.
- README now includes a JSON-RPC 2.0 overview, specification links, and a Mermaid protocol flow diagram.
- README dependency section now includes Maven and Gradle examples, and the Mermaid flow syntax was simplified for parser compatibility.
- Runtime hot paths optimized: dispatcher now uses interceptor fast-path checks and pre-sized batch response buffers.
- WebMVC endpoint now checks payload size before whitespace scan and reduces repeated batch response list access.
- Auto-configuration internals reorganized into `com.limehee.jsonrpc.spring.boot.autoconfigure.support` package.
- Metrics interceptor latency recording now avoids per-call `Timer.builder(...)` and `Duration` allocations.
- Configuration handling now fails fast for invalid values (`path`, `max-batch-size`, `max-request-bytes`, method list entries) to prevent silent misconfiguration.
- Spring Boot configuration metadata generation is now enabled via configuration processor, with additional IDE hints in `additional-spring-configuration-metadata.json`.
- Notification interception now runs on the executor thread for notifications, ensuring async metrics timing is accurate.
- Notification executor selection is deterministic: explicit bean-name override, then single `Executor`, then `applicationTaskExecutor`, else direct execution.
- Core default exception resolver now hides error `data` unless explicitly enabled.
- `jsonrpc-core` now exports JSpecify as an API dependency so downstream consumers get nullness annotations transitively.
- Added Gradle `apiCompat` verification task (JApiCmp) to check binary compatibility against a baseline release version.
- CI/Publish workflows now run API compatibility checks automatically when baseline tags are available.
- Added JMH benchmark support in `jsonrpc-core` with dispatcher benchmark scenarios.
- Added `docs/release-checklist.md` for repeatable release execution.
- `jsonrpc-spring-boot-autoconfigure` now includes Micrometer runtime dependency so optional metrics conditions can be evaluated safely in consumer applications.

### Fixed
- Invalid id-less requests now correctly return JSON-RPC error responses while valid notifications remain no-response.
- Error mapping tightened so only explicit `JsonRpcException(-32602)` paths produce `Invalid params`; generic runtime exceptions now map to `Internal error` (-32603).
- WebMVC endpoint response serialization now produces stable JSON-RPC payloads even when non-Jackson message converters are active.
