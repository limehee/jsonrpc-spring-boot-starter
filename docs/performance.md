# Performance

This document captures current performance-oriented design choices and measurement options.

## Current Optimizations

- Dispatcher interceptor fast-path (`hasInterceptors`) avoids loop overhead when none are registered.
- Batch dispatch pre-sizes response list with batch size.
- WebMVC endpoint validates payload size before JSON parse.
- Metrics interceptor avoids per-call timer builder allocation.

## Notification Throughput Strategy

Notifications can run async using `jsonrpc.notification-executor-enabled=true`.

Recommended production setup:

1. Provide dedicated `Executor` bean for JSON-RPC notifications.
2. Set `jsonrpc.notification-executor-bean-name` explicitly.
3. Tune pool size/queue based on notification traffic profile.

## Benchmarking

JMH benchmark exists in `jsonrpc-core`:

```bash
./gradlew :jsonrpc-core:jmh
```

It includes dispatcher scenarios for single success/error/invalid cases and large batch profiles (all-success, all-error, mixed, notification-only).

Quick profile (short warmup/measurement):

```bash
./gradlew :jsonrpc-core:jmhQuick
```

Run quick profile for a specific benchmark include pattern:

```bash
./gradlew :jsonrpc-core:jmhQuick -PjmhQuickInclude=JsonRpcDispatcherBenchmark.dispatchSingle
```

## Practical Tuning Checklist

- Adjust `jsonrpc.max-request-bytes` to realistic payload limits.
- Adjust `jsonrpc.max-batch-size` to protect CPU spikes.
- Keep `include-error-data=false` in production unless required.
- Disable metrics if not needed (`jsonrpc.metrics-enabled=false`).
- Use allowlist/denylist to reduce exposed method surface area.
- Set `jsonrpc.metrics-max-method-tag-values` to bound method tag cardinality.
- Enable histogram/percentiles only when needed:
  - `jsonrpc.metrics-latency-histogram-enabled`
  - `jsonrpc.metrics-latency-percentiles`

## Performance Testing Guidance

For representative results, test with:

- mixed success/error/notification traffic
- realistic payload sizes (not only small DTOs)
- transport-level concurrency similar to deployment
- executor saturation and queue pressure for notifications
