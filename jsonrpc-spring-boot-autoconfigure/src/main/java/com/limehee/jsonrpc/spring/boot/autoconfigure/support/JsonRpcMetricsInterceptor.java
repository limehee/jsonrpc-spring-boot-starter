package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class JsonRpcMetricsInterceptor implements JsonRpcInterceptor {

    private static final String CALLS_METRIC = "jsonrpc.server.calls";
    private static final String LATENCY_METRIC = "jsonrpc.server.latency";
    private static final String STAGE_EVENTS_METRIC = "jsonrpc.server.stage.events";
    private static final String FAILURE_METRIC = "jsonrpc.server.failures";
    private static final String METHOD_OTHER = "other";
    private static final String METHOD_UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;
    private final boolean latencyHistogramEnabled;
    private final double[] latencyPercentiles;
    private final int maxMethodTagValues;
    private final Set<String> seenMethods = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<LatencyKey, Timer> latencyTimers = new ConcurrentHashMap<>();
    private final ThreadLocal<Long> startedAtNanos = new ThreadLocal<>();

    public JsonRpcMetricsInterceptor(MeterRegistry meterRegistry) {
        this(meterRegistry, false, new double[0], 100);
    }

    public JsonRpcMetricsInterceptor(
            MeterRegistry meterRegistry,
            boolean latencyHistogramEnabled,
            double[] latencyPercentiles,
            int maxMethodTagValues
    ) {
        this.meterRegistry = meterRegistry;
        this.latencyHistogramEnabled = latencyHistogramEnabled;
        this.latencyPercentiles = latencyPercentiles == null ? new double[0] : latencyPercentiles.clone();
        this.maxMethodTagValues = maxMethodTagValues;
    }

    @Override
    public void beforeInvoke(JsonRpcRequest request) {
        startedAtNanos.set(System.nanoTime());
    }

    @Override
    public void afterInvoke(JsonRpcRequest request, JsonNode result) {
        String method = normalizeMethodName(request == null ? null : request.method());
        recordCallAndLatency(method, "success", "none");
        meterRegistry.counter(STAGE_EVENTS_METRIC,
                "method", method,
                "stage", "invoke_success").increment();
    }

    @Override
    public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
        String method = normalizeMethodName(request == null ? null : request.method());
        String errorCode = mappedError == null ? "unknown" : String.valueOf(mappedError.code());
        recordCallAndLatency(method, "error", errorCode);

        String stage = classifyStage(mappedError);
        meterRegistry.counter(STAGE_EVENTS_METRIC,
                "method", method,
                "stage", stage).increment();

        String source = classifyFailureSource(throwable, mappedError);
        meterRegistry.counter(FAILURE_METRIC,
                "method", method,
                "errorCode", errorCode,
                "source", source).increment();
    }

    private void recordCallAndLatency(String method, String outcome, String errorCode) {
        meterRegistry.counter(CALLS_METRIC,
                "method", method,
                "outcome", outcome,
                "errorCode", errorCode).increment();

        Long startNanos = startedAtNanos.get();
        startedAtNanos.remove();
        if (startNanos != null) {
            long elapsedNanos = Math.max(0L, System.nanoTime() - startNanos);
            latencyTimer(method, outcome).record(elapsedNanos, TimeUnit.NANOSECONDS);
        }
    }

    private Timer latencyTimer(String method, String outcome) {
        LatencyKey key = new LatencyKey(method, outcome);
        return latencyTimers.computeIfAbsent(key, ignored -> {
            Timer.Builder builder = Timer.builder(LATENCY_METRIC)
                    .tag("method", method)
                    .tag("outcome", outcome);
            if (latencyHistogramEnabled) {
                builder.publishPercentileHistogram();
            }
            if (latencyPercentiles.length > 0) {
                builder.publishPercentiles(latencyPercentiles);
            }
            return builder.register(meterRegistry);
        });
    }

    private String classifyStage(JsonRpcError mappedError) {
        if (mappedError == null) {
            return "unknown_error";
        }
        return switch (mappedError.code()) {
            case JsonRpcErrorCode.INVALID_REQUEST -> "invalid_request";
            case JsonRpcErrorCode.METHOD_NOT_FOUND -> "method_not_found";
            case JsonRpcErrorCode.INVALID_PARAMS -> "invalid_params";
            case JsonRpcErrorCode.INTERNAL_ERROR -> "internal_error";
            default -> "custom_error";
        };
    }

    private String classifyFailureSource(Throwable throwable, JsonRpcError mappedError) {
        if (mappedError == null) {
            return "unknown";
        }

        int code = mappedError.code();
        if (code == JsonRpcErrorCode.INVALID_REQUEST) {
            return "validation";
        }
        if (code == JsonRpcErrorCode.INVALID_PARAMS) {
            return "binding";
        }
        if (code == JsonRpcErrorCode.METHOD_NOT_FOUND) {
            if (containsClass(throwable, "JsonRpcMethodAccessInterceptor")) {
                return "access_control";
            }
            return "resolution";
        }
        if (code == JsonRpcErrorCode.INTERNAL_ERROR) {
            if (containsClass(throwable, "Interceptor")) {
                return "interceptor";
            }
            return "handler";
        }
        return "custom";
    }

    private boolean containsClass(Throwable throwable, String classNameFragment) {
        if (throwable == null) {
            return false;
        }
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (element.getClassName().contains(classNameFragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeMethodName(String method) {
        if (method == null || method.isBlank()) {
            return METHOD_UNKNOWN;
        }
        if (maxMethodTagValues <= 0) {
            return method;
        }
        if (seenMethods.contains(method)) {
            return method;
        }
        if (seenMethods.size() >= maxMethodTagValues) {
            return METHOD_OTHER;
        }
        seenMethods.add(method);
        return method;
    }

    private record LatencyKey(String method, String outcome) {
    }
}
