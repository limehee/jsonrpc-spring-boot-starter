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

### Changed
- JSON-RPC compliance handling for batch, notification, and invalid request edge cases.
- HTTP endpoint behavior: JSON content-type enforcement and request size limit handling.
- Build now uses Gradle Version Catalog (`gradle/libs.versions.toml`).
- Core API nullability contracts now use JSpecify annotations.
- GitHub Actions updated to latest major/patch releases and Gradle configuration cache enabled in CI/publish workflows.
- Strict JSON-RPC alignment: reserved `rpc.*` methods are always rejected and parse-only-whitespace payloads now return `Parse error` (-32700).
- README reorganized for better onboarding with basic vs advanced usage sections.
- Spring configuration metadata now clarifies that `rpc.*` methods are always reserved.
- README now includes a JSON-RPC 2.0 overview, specification links, and a Mermaid protocol flow diagram.

### Fixed
- Invalid id-less requests now correctly return JSON-RPC error responses while valid notifications remain no-response.
- Error mapping tightened so only explicit `JsonRpcException(-32602)` paths produce `Invalid params`; generic runtime exceptions now map to `Internal error` (-32603).
