# Configuration Reference

All configuration keys are under `jsonrpc.*`.

## Properties

| Key | Type | Default | Description |
|---|---|---|---|
| `jsonrpc.enabled` | `boolean` | `true` | Enable/disable WebMVC endpoint auto-configuration |
| `jsonrpc.path` | `String` | `/jsonrpc` | JSON-RPC HTTP endpoint path |
| `jsonrpc.max-batch-size` | `int` | `100` | Maximum items in batch request |
| `jsonrpc.max-request-bytes` | `int` | `1048576` | Raw request payload size limit in bytes |
| `jsonrpc.scan-annotated-methods` | `boolean` | `true` | Scan Spring beans for `@JsonRpcMethod` |
| `jsonrpc.include-error-data` | `boolean` | `false` | Include `JsonRpcException.data` in error payload |
| `jsonrpc.metrics-enabled` | `boolean` | `true` | Enable Micrometer interceptor when registry exists |
| `jsonrpc.metrics-latency-histogram-enabled` | `boolean` | `false` | Enable latency histogram publication for JSON-RPC metrics |
| `jsonrpc.metrics-latency-percentiles` | `List<Double>` | `[]` | Optional latency percentiles (each value must be `0.0 < p < 1.0`) |
| `jsonrpc.metrics-max-method-tag-values` | `int` | `100` | Maximum unique method tags; extra methods are grouped as `other` |
| `jsonrpc.notification-executor-enabled` | `boolean` | `false` | Enable executor-backed notification invocation |
| `jsonrpc.notification-executor-bean-name` | `String` | `""` | Explicit executor bean name for notification execution |
| `jsonrpc.method-registration-conflict-policy` | `REJECT` or `REPLACE` | `REJECT` | Duplicate method registration behavior |
| `jsonrpc.method-allowlist` | `List<String>` | `[]` | Allowed methods (empty means allow all) |
| `jsonrpc.method-denylist` | `List<String>` | `[]` | Denied methods (takes precedence) |

## Validation Rules (Fail Fast)

Application startup fails if:

- `jsonrpc.path` is blank
- `jsonrpc.path` does not start with `/`
- `jsonrpc.path` contains whitespace
- `jsonrpc.max-batch-size <= 0`
- `jsonrpc.max-request-bytes <= 0`
- `jsonrpc.method-registration-conflict-policy == null`
- `jsonrpc.metrics-max-method-tag-values <= 0`
- `jsonrpc.metrics-latency-percentiles` is null
- percentile value is null, `<= 0.0`, or `>= 1.0`
- `jsonrpc.notification-executor-bean-name == null`
- `jsonrpc.method-allowlist` or `jsonrpc.method-denylist` is null
- allowlist/denylist contains null or blank entry

## IDE Auto-completion and Hints

Spring Boot metadata is provided by:

- configuration processor dependency
- `META-INF/additional-spring-configuration-metadata.json`

This enables IDE hints for:

- property names
- enum values (`REJECT`, `REPLACE`)
- recommended path examples
- suggested notification executor bean names

## Example `application.yml`

```yaml
jsonrpc:
  enabled: true
  path: /jsonrpc
  max-batch-size: 100
  max-request-bytes: 1048576
  scan-annotated-methods: true
  include-error-data: false
  metrics-enabled: true
  metrics-latency-histogram-enabled: false
  metrics-latency-percentiles: [0.9, 0.95, 0.99]
  metrics-max-method-tag-values: 100
  notification-executor-enabled: true
  notification-executor-bean-name: applicationTaskExecutor
  method-registration-conflict-policy: REJECT
  method-allowlist: []
  method-denylist: []
```

## Access Policy Behavior

- If allowlist is empty: all methods are allowed unless in denylist.
- If allowlist is non-empty: only listed methods are allowed.
- Denylist always overrides allowlist.
- `rpc.*` methods are blocked by registry regardless of lists.

## Payload Size Enforcement

`jsonrpc.max-request-bytes` is checked before JSON parsing in WebMVC endpoint.

If exceeded:

- protocol error response is returned (`-32600`)
- default HTTP status is `200` (overridable by custom status strategy)
