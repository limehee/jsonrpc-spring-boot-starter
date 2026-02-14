package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;

class JsonRpcMetricsInterceptor implements JsonRpcInterceptor {

    private static final String CALLS_METRIC = "jsonrpc.server.calls";
    private static final String LATENCY_METRIC = "jsonrpc.server.latency";

    private final MeterRegistry meterRegistry;
    private final ThreadLocal<Long> startedAtNanos = new ThreadLocal<>();

    JsonRpcMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeInvoke(JsonRpcRequest request) {
        startedAtNanos.set(System.nanoTime());
    }

    @Override
    public void afterInvoke(JsonRpcRequest request, JsonNode result) {
        record(request, "success", "none");
    }

    @Override
    public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
        String errorCode = mappedError == null ? "unknown" : String.valueOf(mappedError.code());
        record(request, "error", errorCode);
    }

    private void record(JsonRpcRequest request, String outcome, String errorCode) {
        String method = request == null || request.method() == null || request.method().isBlank()
                ? "unknown"
                : request.method();

        meterRegistry.counter(CALLS_METRIC,
                "method", method,
                "outcome", outcome,
                "errorCode", errorCode).increment();

        Long startNanos = startedAtNanos.get();
        startedAtNanos.remove();
        if (startNanos != null) {
            long elapsedNanos = Math.max(0L, System.nanoTime() - startNanos);
            Timer.builder(LATENCY_METRIC)
                    .tag("method", method)
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(Duration.ofNanos(elapsedNanos));
        }
    }
}
