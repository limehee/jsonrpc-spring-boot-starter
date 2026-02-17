# Configuration Reference

All properties are under `jsonrpc.*` and are bound to `JsonRpcProperties`.

## 1. Property Table

| Key                                           | Type                  | Default    | Description                                                     |
|-----------------------------------------------|-----------------------|------------|-----------------------------------------------------------------|
| `jsonrpc.enabled`                             | `boolean`             | `true`     | Enable/disable WebMVC endpoint auto-configuration               |
| `jsonrpc.path`                                | `String`              | `/jsonrpc` | JSON-RPC HTTP endpoint path                                     |
| `jsonrpc.max-batch-size`                      | `int`                 | `100`      | Maximum number of entries allowed in one batch request          |
| `jsonrpc.max-request-bytes`                   | `int`                 | `1048576`  | Raw HTTP request payload size limit in bytes                    |
| `jsonrpc.scan-annotated-methods`              | `boolean`             | `true`     | Scan Spring beans for `@JsonRpcMethod`                          |
| `jsonrpc.include-error-data`                  | `boolean`             | `false`    | Include `JsonRpcException.data` in error responses              |
| `jsonrpc.method-registration-conflict-policy` | `REJECT` or `REPLACE` | `REJECT`   | Duplicate method name registration policy                       |
| `jsonrpc.method-allowlist`                    | `List<String>`        | `[]`       | Allowlist for method access filtering                           |
| `jsonrpc.method-denylist`                     | `List<String>`        | `[]`       | Denylist for method access filtering (higher priority)          |
| `jsonrpc.metrics-enabled`                     | `boolean`             | `true`     | Enable Micrometer interceptor/observer when registry is present |
| `jsonrpc.metrics-latency-histogram-enabled`   | `boolean`             | `false`    | Publish latency histogram buckets                               |
| `jsonrpc.metrics-latency-percentiles`         | `List<Double>`        | `[]`       | Optional latency percentiles (`0.0 < p < 1.0`)                  |
| `jsonrpc.metrics-max-method-tag-values`       | `int`                 | `100`      | Max distinct method tag values before fallback to `other`       |
| `jsonrpc.notification-executor-enabled`       | `boolean`             | `false`    | Enable executor-backed notification dispatch                    |
| `jsonrpc.notification-executor-bean-name`     | `String`              | `""`       | Preferred executor bean name for notifications                  |

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
- allowlist/denylist list itself is null
- allowlist/denylist contains null or blank values

## 3. Runtime Behavior Priority

## 3.1 Method access filtering

Priority:

1. Denylist check
2. Allowlist check
3. Default allow when allowlist is empty

Rules:

- denylist always overrides allowlist
- if allowlist is non-empty, methods not in allowlist are denied
- `rpc.*` methods are blocked by registry independently of allow/deny lists

## 3.2 Notification executor resolution

When `jsonrpc.notification-executor-enabled=true`, resolution order is:

1. explicit `jsonrpc.notification-executor-bean-name`
2. single `Executor` bean in context
3. bean named `applicationTaskExecutor`
4. fallback to direct executor

If a configured bean name is missing, startup fails.

## 3.3 Method registration conflict handling

- `REJECT`: first duplicate fails registration.
- `REPLACE`: later registration wins.

In auto-configuration, annotation scanning runs after manual registrations, so annotation handlers can replace manual handlers under `REPLACE`.

## 4. Property Source Precedence (Spring Boot)

Effective value follows standard Spring Boot externalized configuration precedence. Typical order (high to low):

1. command-line arguments (`--jsonrpc.path=/rpc`)
2. environment variables (`JSONRPC_PATH=/rpc`)
3. `application-<profile>.yml`
4. `application.yml`

Example environment variable mapping:

- `jsonrpc.max-request-bytes` -> `JSONRPC_MAX_REQUEST_BYTES`
- `jsonrpc.method-registration-conflict-policy` -> `JSONRPC_METHOD_REGISTRATION_CONFLICT_POLICY`

## 5. Example Configurations

## 5.1 Baseline production profile

```yaml
jsonrpc:
  path: /jsonrpc
  max-batch-size: 100
  max-request-bytes: 1048576
  method-registration-conflict-policy: REJECT
  scan-annotated-methods: true
  include-error-data: false
  method-allowlist: []
  method-denylist: []
```

## 5.2 Strict access profile

```yaml
jsonrpc:
  method-allowlist: [user.find, user.update]
  method-denylist: [user.delete]
```

## 5.3 Async notification profile

```yaml
jsonrpc:
  notification-executor-enabled: true
  notification-executor-bean-name: applicationTaskExecutor
```

## 5.4 Metrics-rich profile

```yaml
jsonrpc:
  metrics-enabled: true
  metrics-latency-histogram-enabled: true
  metrics-latency-percentiles: [0.9, 0.95, 0.99]
  metrics-max-method-tag-values: 200
```

## 6. IDE Auto-completion and Metadata

The project ships Spring Boot configuration metadata via:

- `spring-boot-configuration-processor`
- `META-INF/additional-spring-configuration-metadata.json`

This enables:

- property key completion
- enum value suggestions (`REJECT`, `REPLACE`)
- metadata hints in IntelliJ and Spring-aware tooling

## 7. Related References

- Spring setup details: [`spring-boot-guide.md`](spring-boot-guide.md)
- Binding and registration details: [`registration-and-binding.md`](registration-and-binding.md)
- Extension interfaces: [`extension-points.md`](extension-points.md)
