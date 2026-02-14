# JSON-RPC 2.0 Library + Spring Boot Starter Plan

## 1) Goal
- Build a JSON-RPC 2.0 compliant server library.
- Provide Spring Boot starter for fast adoption.
- Keep core transport-agnostic and modular for long-term maintainability.
- Design explicit extension points (method binding, validation, error mapping, observability).

## 2) Scope
- JSON-RPC 2.0 request/response processing (single + batch).
- Notification support (no response).
- Standard error model and code handling.
- Spring MVC HTTP endpoint integration via auto-configuration.
- Production-ready defaults (limits, metrics, logging hooks, configuration properties).

## 3) Non-goals (v1)
- Client SDK.
- WebSocket/SSE transport.
- JSON-RPC 1.0 compatibility mode.
- Distributed method registry.

## 4) JSON-RPC 2.0 Compliance Matrix (MUST)
- `jsonrpc` MUST be exactly `"2.0"`.
- Request object MUST contain `method` (string).
- `id` handling:
  - present: response required (unless parse-level failure before id extraction).
  - absent: notification, no response body.
- `params` MAY be object, array, or omitted.
- Response object MUST include either `result` or `error` (mutually exclusive).
- Error object MUST include `code`, `message` and optional `data`.
- Standard error codes:
  - `-32700` Parse error
  - `-32600` Invalid Request
  - `-32601` Method not found
  - `-32602` Invalid params
  - `-32603` Internal error
- Batch requests:
  - array input supported.
  - empty array => single `Invalid Request` error response.
  - batch response includes only non-notification results.
  - if all entries are notifications => no response body.
- Reserved method namespace `rpc.*` policy configurable (default: reject external registration).

## 5) Proposed Module Architecture
Current repository already has:
- `jsonrpc-core`
- `jsonrpc-spring-boot-autoconfigure`
- `jsonrpc-spring-boot-starter`

Target structure (compatible evolution):
- `jsonrpc-core`
  - protocol model, dispatcher pipeline, registry, validation, error mapping.
- `jsonrpc-spring-webmvc` (new, optional extraction)
  - HTTP transport adapter/controller layer only.
- `jsonrpc-spring-boot-autoconfigure`
  - bean wiring, properties binding, conditional configuration.
- `jsonrpc-spring-boot-starter`
  - dependency bundle.

If module count should stay minimal, keep 3 modules and separate packages internally with the same boundaries.

## 6) Core Design (Extensible)
### 6.1 Protocol Model
- Immutable models preferred (`record` or immutable class):
  - `JsonRpcRequest`
  - `JsonRpcResponse`
  - `JsonRpcError`
- Keep raw JSON representation (`JsonNode`) at core boundary for transport neutrality.

### 6.2 Dispatcher Pipeline
Split processing into components:
- `JsonRpcParser` (JSON to envelope: single/batch)
- `JsonRpcRequestValidator`
- `JsonRpcMethodRegistry`
- `JsonRpcInvoker`
- `JsonRpcExceptionResolver`
- `JsonRpcResponseComposer`

`JsonRpcDispatcher` orchestrates only; each stage replaceable.

### 6.3 Method Binding
Support two registration styles:
- Low-level:
  - `JsonRpcMethodHandler` (`JsonNode -> JsonNode`) for full control.
- Typed adapter (recommended):
  - map `params` to DTO, invoke typed function, map return value to JSON.
  - powered by pluggable `ParameterBinder` + `ResultWriter`.

### 6.4 Error Strategy
- `JsonRpcException` base type with explicit `code/message/data`.
- Default resolver mapping:
  - validation failure -> `-32600`
  - method missing -> `-32601`
  - binding/argument failure -> `-32602`
  - unhandled error -> `-32603`
- Extension point for domain-specific errors in `-32000..-32099` server range.

## 7) Spring Boot Starter Design
### 7.1 Auto-configuration
- Provide default beans only when missing:
  - dispatcher
  - registry
  - validator
  - exception resolver
  - webmvc endpoint adapter
- Collect all `JsonRpcMethodRegistration` beans and register in deterministic order.

### 7.2 Endpoint Behavior
- Default endpoint: `POST /jsonrpc`.
- Content-Type: `application/json`.
- Parse failures mapped to `-32700` with valid JSON-RPC error payload.
- HTTP status policy:
  - default: `200 OK` for protocol-level errors.
  - no content for pure notification calls.
  - make status strategy overridable.

### 7.3 Configuration Properties
`jsonrpc.*`:
- `enabled` (default `true`)
- `path` (default `/jsonrpc`)
- `max-batch-size` (default `100`)
- `max-request-bytes` (default `1MB`)
- `notification-executor-enabled`
- `method-namespace-policy` (`ALLOW_ALL`, `DISALLOW_RPC_PREFIX`)
- `include-error-data` (sensitive data exposure control)

## 8) Cross-cutting Extension Points
- Interceptor chain:
  - before-validate
  - before-invoke
  - after-invoke
  - on-error
- Observability:
  - Micrometer timer/counter by `method`, `resultType`, `errorCode`.
  - structured logging with masking hooks.
- Security:
  - method allowlist/denylist.
  - payload and batch limits.
  - optional auth context propagation hook.

## 9) Testing Strategy
- Unit tests (core components isolated).
- Compliance test suite (spec scenarios):
  - parse error, invalid request, notification, batch mixed cases.
- Spring integration tests:
  - auto-configuration wiring
  - endpoint behavior and properties
- Compatibility tests across supported Spring Boot versions.

## 10) Delivery Phases
### Phase 1: Core compliance hardening
- Add batch model + parser + validator.
- Introduce exception resolver and standardized errors.
- Expand unit tests to full compliance matrix.

### Phase 2: Spring adapter refinement
- Move controller logic to transport adapter.
- Add parse-error handling and status strategy.
- Add configurable request limits and namespace policy.

### Phase 3: Extension and observability
- Add interceptor API and metrics instrumentation.
- Add typed binding API.
- Improve docs and sample app.

### Phase 4: Release readiness
- API review and binary compatibility check.
- publish to Maven Central (signed artifacts, sources/javadocs).
- changelog + migration notes.

## 11) Acceptance Criteria (v1)
- All MUST items in Section 4 pass automated tests.
- No transport code leaks into `jsonrpc-core`.
- Spring Boot starter works with zero config for basic use.
- New method registration requires no framework internals change.
- Public extension interfaces documented with examples.

## 12) Current Codebase Gap Summary
Based on current implementation:
- good: baseline dispatcher, error codes, starter/autoconfigure skeleton exist.
- missing for full spec: batch handling, parse-error path, richer validation, response strategy abstraction.
- needed for maintainability: pluggable pipeline interfaces and transport separation.

This plan uses existing modules as foundation and upgrades toward strict JSON-RPC 2.0 compliance with extensible architecture.
