package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcInterceptorExecutionException;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Micrometer-backed interceptor that records server-side JSON-RPC execution metrics.
 * <p>
 * Metrics include:
 * </p>
 * <ul>
 * <li>request call counts by method, outcome, and error code</li>
 * <li>latency timers by method and outcome</li>
 * <li>stage event counts for high-level failure classification</li>
 * <li>failure source counts for root-cause grouping</li>
 * </ul>
 * <p>
 * Method tag cardinality can be bounded by {@code maxMethodTagValues}; once the limit is reached,
 * unseen methods are collapsed into the {@code other} bucket.
 * </p>
 */
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

    /**
     * Creates an interceptor with default metric options.
     *
     * @param meterRegistry registry where JSON-RPC metrics are published
     */
    public JsonRpcMetricsInterceptor(MeterRegistry meterRegistry) {
        this(meterRegistry, false, new double[0], 100);
    }

    /**
     * Creates an interceptor with explicit metric options.
     *
     * @param meterRegistry           registry where JSON-RPC metrics are published
     * @param latencyHistogramEnabled whether latency histogram buckets should be emitted
     * @param latencyPercentiles      latency percentiles to publish for timers
     * @param maxMethodTagValues      maximum number of distinct method tag values before collapsing to {@code other};
     *                                must be greater than {@code 0}
     * @throws IllegalArgumentException if {@code maxMethodTagValues <= 0}
     */
    public JsonRpcMetricsInterceptor(
        MeterRegistry meterRegistry,
        boolean latencyHistogramEnabled,
        double[] latencyPercentiles,
        int maxMethodTagValues
    ) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.latencyHistogramEnabled = latencyHistogramEnabled;
        this.latencyPercentiles = Objects.requireNonNull(latencyPercentiles, "latencyPercentiles").clone();
        if (maxMethodTagValues <= 0) {
            throw new IllegalArgumentException("maxMethodTagValues must be greater than 0");
        }
        this.maxMethodTagValues = maxMethodTagValues;
    }

    /**
     * Captures request start time for latency measurement.
     *
     * @param request JSON-RPC request being invoked
     */
    @Override
    public void beforeInvoke(JsonRpcRequest request) {
        startedAtNanos.set(System.nanoTime());
    }

    /**
     * Records success counters and invocation latency after successful method execution.
     *
     * @param request JSON-RPC request that completed successfully
     * @param result  JSON result produced by the method handler
     */
    @Override
    public void afterInvoke(JsonRpcRequest request, JsonNode result) {
        String method = normalizeMethodName(request.method());
        recordCallAndLatency(method, "success", "none");
        counter(stageCounters, STAGE_EVENTS_METRIC, method, "invoke_success", "").increment();
    }

    /**
     * Records error counters, latency, stage, and failure-source dimensions.
     *
     * @param request     request being processed when the error occurred
     * @param throwable   original throwable associated with the failure
     * @param mappedError protocol-level error mapped for the response
     */
    @Override
    public void onError(@Nullable JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
        JsonRpcError error = Objects.requireNonNull(mappedError, "mappedError");
        String method = normalizeMethodName(request == null ? null : request.method());
        String errorCode = String.valueOf(error.code());
        recordCallAndLatency(method, "error", errorCode);

        String stage = classifyStage(error);
        counter(stageCounters, STAGE_EVENTS_METRIC, method, stage, "").increment();

        String source = classifyFailureSource(throwable, error);
        counter(failureCounters, FAILURE_METRIC, method, errorCode, source).increment();
    }

    /**
     * Records call counter and elapsed time from {@link #beforeInvoke(JsonRpcRequest)}.
     *
     * @param method    normalized method tag value
     * @param outcome   request outcome tag value
     * @param errorCode JSON-RPC error code tag value or semantic placeholder
     */
    private void recordCallAndLatency(String method, String outcome, String errorCode) {
        counter(callCounters, CALLS_METRIC, method, outcome, errorCode).increment();

        Long startNanos = startedAtNanos.get();
        startedAtNanos.remove();
        if (startNanos != null) {
            long elapsedNanos = Math.max(0L, System.nanoTime() - startNanos);
            latencyTimer(method, outcome).record(elapsedNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Resolves or creates a latency timer for the given method/outcome pair.
     *
     * @param method  normalized method tag value
     * @param outcome outcome tag value
     * @return cached or newly registered timer
     */
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

    /**
     * Classifies errors into high-level processing stages.
     *
     * @param mappedError protocol error mapped for the response
     * @return stage label used in stage-event metrics
     */
    private String classifyStage(JsonRpcError mappedError) {
        return switch (mappedError.code()) {
            case JsonRpcErrorCode.INVALID_REQUEST -> "invalid_request";
            case JsonRpcErrorCode.METHOD_NOT_FOUND -> "method_not_found";
            case JsonRpcErrorCode.INVALID_PARAMS -> "invalid_params";
            case JsonRpcErrorCode.INTERNAL_ERROR -> "internal_error";
            default -> "custom_error";
        };
    }

    /**
     * Classifies failure source based on throwable type and mapped protocol error.
     *
     * @param throwable   original throwable associated with the failure
     * @param mappedError protocol error mapped for the response
     * @return source label used in failure metrics
     */
    private String classifyFailureSource(Throwable throwable, JsonRpcError mappedError) {
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

    /**
     * Resolves or creates counter meters for the target metric family.
     *
     * @param cache          in-memory counter cache per metric dimensions
     * @param metricName     metric family name
     * @param method         method tag value
     * @param firstTagValue  first dimension tag value (semantic depends on metric family)
     * @param secondTagValue second dimension tag value (semantic depends on metric family)
     * @return cached or newly registered counter
     */
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

    /**
     * Applies method tag normalization and cardinality limiting.
     *
     * @param method raw method name from request
     * @return normalized method tag value
     */
    private String normalizeMethodName(@Nullable String method) {
        if (method == null || method.isBlank()) {
            return METHOD_UNKNOWN;
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

    /**
     * Cache key for counters differentiated by metric and tag dimensions.
     *
     * @param metric metric family name
     * @param method method tag value
     * @param first  first metric-specific tag value
     * @param second second metric-specific tag value
     */
    private record CounterKey(String metric, String method, String first, String second) {

    }

    /**
     * Cache key for latency timers differentiated by method and outcome.
     *
     * @param method  method tag value
     * @param outcome outcome tag value
     */
    private record LatencyKey(String method, String outcome) {

    }
}
