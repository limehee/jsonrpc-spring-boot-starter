package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcInterceptorExecutionException;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import io.micrometer.core.instrument.Counter;
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
    private final ConcurrentHashMap<CounterKey, Counter> callCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CounterKey, Counter> stageCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<CounterKey, Counter> failureCounters = new ConcurrentHashMap<>();
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
        counter(stageCounters, STAGE_EVENTS_METRIC, method, "invoke_success", "").increment();
    }

    @Override
    public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
        String method = normalizeMethodName(request == null ? null : request.method());
        String errorCode = mappedError == null ? "unknown" : String.valueOf(mappedError.code());
        recordCallAndLatency(method, "error", errorCode);

        String stage = classifyStage(mappedError);
        counter(stageCounters, STAGE_EVENTS_METRIC, method, stage, "").increment();

        String source = classifyFailureSource(throwable, mappedError);
        counter(failureCounters, FAILURE_METRIC, method, errorCode, source).increment();
    }

    private void recordCallAndLatency(String method, String outcome, String errorCode) {
        counter(callCounters, CALLS_METRIC, method, outcome, errorCode).increment();

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
            if (throwable instanceof JsonRpcMethodAccessDeniedException) {
                return "access_control";
            }
            return "resolution";
        }
        if (code == JsonRpcErrorCode.INTERNAL_ERROR) {
            if (throwable instanceof JsonRpcInterceptorExecutionException) {
                return "interceptor";
            }
            return "handler";
        }
        return "custom";
    }

    private Counter counter(
            ConcurrentHashMap<CounterKey, Counter> cache,
            String metricName,
            String method,
            String firstTagValue,
            String secondTagValue
    ) {
        CounterKey key = new CounterKey(metricName, method, firstTagValue, secondTagValue);
        return cache.computeIfAbsent(key, ignored -> {
            if (metricName.equals(CALLS_METRIC)) {
                return meterRegistry.counter(
                        metricName,
                        "method", method,
                        "outcome", firstTagValue,
                        "errorCode", secondTagValue
                );
            }
            if (metricName.equals(STAGE_EVENTS_METRIC)) {
                return meterRegistry.counter(
                        metricName,
                        "method", method,
                        "stage", firstTagValue
                );
            }
            return meterRegistry.counter(
                    metricName,
                    "method", method,
                    "errorCode", firstTagValue,
                    "source", secondTagValue
            );
        });
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
        if (seenMethods.add(method)) {
            return method;
        }
        return method;
    }

    private record CounterKey(String metric, String method, String first, String second) {
    }

    private record LatencyKey(String method, String outcome) {
    }
}
