# Extension Points

The library is designed so behavior can be changed without forking core classes.

## 1. Core Pipeline Interfaces

You can override any of these with custom Spring beans:

- `JsonRpcRequestParser`
- `JsonRpcRequestValidator`
- `JsonRpcMethodRegistry`
- `JsonRpcMethodInvoker`
- `JsonRpcExceptionResolver`
- `JsonRpcResponseComposer`
- `JsonRpcNotificationExecutor`

## 2. Interceptor Chain

`JsonRpcInterceptor` hooks:

- `beforeValidate(JsonNode rawRequest)`
- `beforeInvoke(JsonRpcRequest request)`
- `afterInvoke(JsonRpcRequest request, JsonNode result)`
- `onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError)`

Notes:

- Interceptors are ordered (`ObjectProvider.orderedStream()`).
- `onError` exceptions are swallowed intentionally to avoid masking protocol responses.
- Access control interceptor runs with highest precedence.

Custom example:

```java
@Bean
JsonRpcInterceptor auditInterceptor() {
    return new JsonRpcInterceptor() {
        @Override
        public void beforeInvoke(JsonRpcRequest request) {
        }
    };
}
```

## 3. Metrics

When `MeterRegistry` is present and `jsonrpc.metrics-enabled=true`, a Micrometer interceptor is registered.

Metrics:

- Counter: `jsonrpc.server.calls`
  - tags: `method`, `outcome`, `errorCode`
- Timer: `jsonrpc.server.latency`
  - tags: `method`, `outcome`
- Counter: `jsonrpc.server.stage.events`
  - tags: `method`, `stage`
- Counter: `jsonrpc.server.failures`
  - tags: `method`, `errorCode`, `source`
- Counter: `jsonrpc.server.transport.errors`
  - tags: `reason` (`parse_error`, `request_too_large`)
- Counter: `jsonrpc.server.batch.requests`
  - tags: `outcome` (`all_success`, `all_error`, `mixed`, `notification_only`)
- Counter: `jsonrpc.server.batch.entries`
  - tags: `outcome` (`success`, `error`, `notification`)
- Summary: `jsonrpc.server.batch.size`
- Timer: `jsonrpc.server.notification.queue.delay`
- Timer: `jsonrpc.server.notification.execution`
- Counter: `jsonrpc.server.notification.submitted`
- Counter: `jsonrpc.server.notification.failed`

## 4. Method Access Control

Default `JsonRpcMethodAccessInterceptor` uses allowlist/denylist.

- denylist dominates allowlist
- violation maps to `-32601 Method not found` (avoids exposing method existence)

You can replace this interceptor bean (`jsonRpcMethodAccessInterceptor`) for custom policy logic.

## 5. HTTP Status Strategy

`JsonRpcHttpStatusStrategy` lets you control transport status policy:

- single call
- batch call
- notification-only response
- parse error
- request-too-large case

Default strategy returns `200` for protocol responses and `204` for notification-only.

## 6. Notification Executor

`JsonRpcNotificationExecutor` controls where notification handlers run.

- `DirectJsonRpcNotificationExecutor`: same thread
- `ExecutorJsonRpcNotificationExecutor`: delegated to Java `Executor`
- `InstrumentedJsonRpcNotificationExecutor`: wraps notification execution for queue/latency/failure metrics when metrics are enabled

You can provide your own implementation for custom backpressure/isolation/retry behavior.
