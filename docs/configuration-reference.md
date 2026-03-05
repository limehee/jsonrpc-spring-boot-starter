# Configuration Reference

All properties are under `jsonrpc.*` and are bound to `JsonRpcProperties`.

## 1. Property Table

| Key                                                             | Type                                  | Default                            | Description                                                          |
|-----------------------------------------------------------------|---------------------------------------|------------------------------------|----------------------------------------------------------------------|
| `jsonrpc.enabled`                                               | `boolean`                             | `true`                             | Enable/disable WebMVC endpoint auto-configuration                    |
| `jsonrpc.path`                                                  | `String`                              | `/jsonrpc`                         | JSON-RPC HTTP endpoint path                                          |
| `jsonrpc.max-batch-size`                                        | `int`                                 | `100`                              | Maximum number of entries allowed in one batch request               |
| `jsonrpc.max-request-bytes`                                     | `int`                                 | `1048576`                          | Raw HTTP request payload size limit in bytes                         |
| `jsonrpc.scan-annotated-methods`                                | `boolean`                             | `true`                             | Scan Spring beans for `@JsonRpcMethod`                               |
| `jsonrpc.include-error-data`                                    | `boolean`                             | `false`                            | Include `JsonRpcException.data` in error responses                   |
| `jsonrpc.validation.request.require-json-rpc-version-20`        | `boolean`                             | `true`                             | Require incoming request `jsonrpc` to equal `"2.0"`                 |
| `jsonrpc.validation.request.require-id-member`                  | `boolean`                             | `false`                            | Require incoming requests to include an `id` member                  |
| `jsonrpc.validation.request.allow-null-id`                      | `boolean`                             | `true`                             | Allow `id: null` in incoming requests                                |
| `jsonrpc.validation.request.allow-string-id`                    | `boolean`                             | `true`                             | Allow string IDs in incoming requests                                |
| `jsonrpc.validation.request.allow-numeric-id`                   | `boolean`                             | `true`                             | Allow numeric IDs in incoming requests                               |
| `jsonrpc.validation.request.allow-fractional-id`                | `boolean`                             | `true`                             | Allow fractional numeric IDs in incoming requests                    |
| `jsonrpc.validation.request.reject-response-fields`             | `boolean`                             | `false`                            | Reject request objects containing `result`/`error`                   |
| `jsonrpc.validation.request.reject-duplicate-members`           | `boolean`                             | `false`                            | Reject duplicate members while parsing raw request JSON              |
| `jsonrpc.validation.request.params-type-violation-code-policy`  | `INVALID_PARAMS` or `INVALID_REQUEST` | `INVALID_PARAMS`                   | Error code used when `params` exists but is neither object nor array |
| `jsonrpc.validation.response.require-json-rpc-version-20`       | `boolean`                             | `true`                             | Require incoming response `jsonrpc` to equal `"2.0"`                 |
| `jsonrpc.validation.response.require-id-member`                 | `boolean`                             | `true`                             | Require incoming responses to include an `id` member                 |
| `jsonrpc.validation.response.allow-null-id`                     | `boolean`                             | `true`                             | Allow `id: null` in incoming responses                               |
| `jsonrpc.validation.response.allow-string-id`                   | `boolean`                             | `true`                             | Allow string IDs in incoming responses                               |
| `jsonrpc.validation.response.allow-numeric-id`                  | `boolean`                             | `true`                             | Allow numeric IDs in incoming responses                              |
| `jsonrpc.validation.response.allow-fractional-id`               | `boolean`                             | `true`                             | Allow fractional numeric IDs in incoming responses                   |
| `jsonrpc.validation.response.require-exclusive-result-or-error` | `boolean`                             | `true`                             | Require exactly one of `result` or `error`                           |
| `jsonrpc.validation.response.require-error-object-when-present` | `boolean`                             | `true`                             | Require `error` to be an object when present                         |
| `jsonrpc.validation.response.require-integer-error-code`        | `boolean`                             | `true`                             | Require `error.code` to be an integer                                |
| `jsonrpc.validation.response.require-string-error-message`      | `boolean`                             | `true`                             | Require `error.message` to be a string                               |
| `jsonrpc.validation.response.reject-request-fields`             | `boolean`                             | `false`                            | Reject response objects containing `method`/`params`                 |
| `jsonrpc.validation.response.reject-duplicate-members`          | `boolean`                             | `false`                            | Reject duplicate members while parsing raw response JSON             |
| `jsonrpc.validation.response.error-code.policy`                 | `JsonRpcResponseErrorCodePolicy`      | `ANY_INTEGER`                      | Accepted integer range policy for response `error.code`              |
| `jsonrpc.validation.response.error-code.range.min`              | `Integer`                             | `null`                             | Inclusive minimum for `CUSTOM_RANGE`                                 |
| `jsonrpc.validation.response.error-code.range.max`              | `Integer`                             | `null`                             | Inclusive maximum for `CUSTOM_RANGE`                                 |
| `jsonrpc.method-registration-conflict-policy`                   | `REJECT` or `REPLACE`                 | `REJECT`                           | Duplicate method name registration policy                            |
| `jsonrpc.method-allowlist`                                      | `List<String>`                        | `[]`                               | Allowlist for method access filtering                                |
| `jsonrpc.method-denylist`                                       | `List<String>`                        | `[]`                               | Denylist for method access filtering (higher priority)               |
| `jsonrpc.metrics-enabled`                                       | `boolean`                             | `true`                             | Enable Micrometer interceptor/observer when registry is present      |
| `jsonrpc.metrics-latency-histogram-enabled`                     | `boolean`                             | `false`                            | Publish latency histogram buckets                                    |
| `jsonrpc.metrics-latency-percentiles`                           | `List<Double>`                        | `[]`                               | Optional latency percentiles (`0.0 < p < 1.0`)                       |
| `jsonrpc.metrics-max-method-tag-values`                         | `int`                                 | `100`                              | Max distinct method tag values before fallback to `other`            |
| `jsonrpc.notification-executor-enabled`                         | `boolean`                             | `false`                            | Enable executor-backed notification dispatch                         |
| `jsonrpc.notification-executor-bean-name`                       | `String`                              | `""`                               | Preferred executor bean name for notifications                       |

`JsonRpcResponseErrorCodePolicy` values:
- `ANY_INTEGER`
- `STANDARD_ONLY`
- `STANDARD_OR_SERVER_ERROR_RANGE`
- `CUSTOM_RANGE`

## 2. Validation Rules (Fail Fast)

Startup fails with `IllegalArgumentException` when any of these conditions occur:

- `jsonrpc.path` is null/blank
- `jsonrpc.path` does not start with `/`
- `jsonrpc.path` contains whitespace
- `jsonrpc.max-batch-size <= 0`
- `jsonrpc.max-request-bytes <= 0`
- `jsonrpc.method-registration-conflict-policy` is null
- `jsonrpc.metrics-max-method-tag-values <= 0`
- `jsonrpc.metrics-latency-percentiles` is null
- any percentile is null, `<= 0.0`, or `>= 1.0`
- `jsonrpc.notification-executor-bean-name` is null
- `jsonrpc.validation` is null
- `jsonrpc.validation.request` is null
- `jsonrpc.validation.request.params-type-violation-code-policy` is null
- `jsonrpc.validation.response` is null
- `jsonrpc.validation.response.error-code` is null
- `jsonrpc.validation.response.error-code.policy` is null
- `jsonrpc.validation.response.error-code.range` is null
- `jsonrpc.validation.response.require-integer-error-code=false` with `jsonrpc.validation.response.error-code.policy != ANY_INTEGER`
- `jsonrpc.validation.response.error-code.policy=CUSTOM_RANGE` and either range bound is missing
- `jsonrpc.validation.response.error-code.policy=CUSTOM_RANGE` and `range.min > range.max`
- allowlist/denylist list itself is null
- allowlist/denylist contains null or blank values

## 3. Runtime Behavior Priority

### 3.1 Method access filtering

Priority:

1. Denylist check
2. Allowlist check
3. Default allow when allowlist is empty

Rules:

- denylist always overrides allowlist
- if allowlist is non-empty, methods not in allowlist are denied
- `rpc.*` methods are blocked by request validation, and also by the default registry independently of allow/deny
  lists

### 3.2 Notification executor resolution

When `jsonrpc.notification-executor-enabled=true`, resolution order is:

1. explicit `jsonrpc.notification-executor-bean-name`
2. single `Executor` bean in context
3. bean named `applicationTaskExecutor`
4. fallback to direct executor

If a configured bean name is missing, startup fails.

### 3.3 Method registration conflict handling

- `REJECT`: first duplicate fails registration.
- `REPLACE`: later registration wins.

In auto-configuration, annotation scanning runs after manual registrations, so annotation handlers can replace manual
handlers under `REPLACE`.

## 4. Property Source Precedence (Spring Boot)

Effective value follows standard Spring Boot externalized configuration precedence. Typical order (high to low):

1. command-line arguments (`--jsonrpc.path=/rpc`)
2. environment variables (`JSONRPC_PATH=/rpc`)
3. `application-<profile>.yml`
4. `application.yml`

Example environment variable mapping:

- `jsonrpc.max-request-bytes` -> `JSONRPC_MAX_REQUEST_BYTES`
- `jsonrpc.method-registration-conflict-policy` -> `JSONRPC_METHOD_REGISTRATION_CONFLICT_POLICY`
- `jsonrpc.validation.request.params-type-violation-code-policy` ->
  `JSONRPC_VALIDATION_REQUEST_PARAMS_TYPE_VIOLATION_CODE_POLICY`

## 5. Example Configurations

### 5.1 Baseline production profile

```yaml
jsonrpc:
  path: /jsonrpc
  max-batch-size: 100
  max-request-bytes: 1048576
  method-registration-conflict-policy: REJECT
  scan-annotated-methods: true
  include-error-data: false
  validation:
    request:
      require-json-rpc-version-20: true
      require-id-member: false
      allow-null-id: true
      allow-string-id: true
      allow-numeric-id: true
      allow-fractional-id: true
      reject-response-fields: false
      reject-duplicate-members: false
      params-type-violation-code-policy: INVALID_PARAMS
    response:
      require-json-rpc-version-20: true
      require-id-member: true
      allow-null-id: true
      allow-string-id: true
      allow-numeric-id: true
      allow-fractional-id: true
      require-exclusive-result-or-error: true
      require-error-object-when-present: true
      require-integer-error-code: true
      require-string-error-message: true
      reject-request-fields: false
      reject-duplicate-members: false
      error-code:
        policy: ANY_INTEGER
        range:
          min: null
          max: null
  method-allowlist: [ ]
  method-denylist: [ ]
```

### 5.2 Strict response error-code profile

```yaml
jsonrpc:
  validation:
    response:
      reject-request-fields: true
      error-code:
        policy: STANDARD_OR_SERVER_ERROR_RANGE
```

### 5.3 Async notification profile

```yaml
jsonrpc:
  notification-executor-enabled: true
  notification-executor-bean-name: applicationTaskExecutor
```

### 5.4 Metrics-rich profile

```yaml
jsonrpc:
  metrics-enabled: true
  metrics-latency-histogram-enabled: true
  metrics-latency-percentiles: [ 0.9, 0.95, 0.99 ]
  metrics-max-method-tag-values: 200
```

## 6. Migration Notes (Response Validation Key Rename)

Old keys replaced by canonical symmetric keys:

| Old key                                                           | New key                                              |
|-------------------------------------------------------------------|------------------------------------------------------|
| `jsonrpc.validation.response.require-response-id-member`          | `jsonrpc.validation.response.require-id-member`      |
| `jsonrpc.validation.response.allow-null-response-id`              | `jsonrpc.validation.response.allow-null-id`          |
| `jsonrpc.validation.response.allow-string-response-id`            | `jsonrpc.validation.response.allow-string-id`        |
| `jsonrpc.validation.response.allow-numeric-response-id`           | `jsonrpc.validation.response.allow-numeric-id`       |
| `jsonrpc.validation.response.allow-fractional-response-id`        | `jsonrpc.validation.response.allow-fractional-id`    |
| `jsonrpc.validation.response.allow-request-fields-in-response`    | `jsonrpc.validation.response.reject-request-fields`  |

Inversion rule:

- `reject-request-fields = !allow-request-fields-in-response`

## 7. IDE Auto-completion and Metadata

The project ships Spring Boot configuration metadata via:

- `spring-boot-configuration-processor`
- `META-INF/additional-spring-configuration-metadata.json`

This enables:

- property key completion
- enum value suggestions (`REJECT`, `REPLACE`, `INVALID_PARAMS`, `INVALID_REQUEST`, error-code policy values)
- example numeric suggestions for `jsonrpc.validation.response.error-code.range.min/max`
- metadata hints in IntelliJ and Spring-aware tooling

Hints are suggestions for IDE completion only. They do not restrict allowed runtime values unless validation rules
explicitly enforce constraints.

## 8. Related References

- Spring setup details: [`spring-boot-guide.md`](spring-boot-guide.md)
- Binding and registration details: [`registration-and-binding.md`](registration-and-binding.md)
- Extension interfaces: [`extension-points.md`](extension-points.md)
