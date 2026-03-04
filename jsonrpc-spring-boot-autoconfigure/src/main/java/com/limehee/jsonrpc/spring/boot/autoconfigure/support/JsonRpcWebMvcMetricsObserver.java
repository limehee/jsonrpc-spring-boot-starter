package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Objects;

/**
 * Micrometer-backed observer for transport-level JSON-RPC WebMVC events.
 * <p>
 * This observer tracks parsing failures, request size violations, notification-only handling, and batch-level
 * composition details.
 * </p>
 */
public final class JsonRpcWebMvcMetricsObserver implements JsonRpcWebMvcObserver {

    private static final String TRANSPORT_ERRORS_METRIC = "jsonrpc.server.transport.errors";
    private static final String NOTIFICATION_METRIC = "jsonrpc.server.transport.notifications";
    private static final String BATCH_REQUEST_METRIC = "jsonrpc.server.batch.requests";
    private static final String BATCH_ENTRY_METRIC = "jsonrpc.server.batch.entries";
    private static final String BATCH_SIZE_METRIC = "jsonrpc.server.batch.size";

    private final Counter parseErrorCounter;
    private final Counter requestTooLargeCounter;
    private final Counter singleNotificationCounter;
    private final Counter batchNotificationCounter;
    private final Counter batchRequestAllSuccessCounter;
    private final Counter batchRequestAllErrorCounter;
    private final Counter batchRequestMixedCounter;
    private final Counter batchRequestNotificationOnlyCounter;
    private final Counter batchEntrySuccessCounter;
    private final Counter batchEntryErrorCounter;
    private final Counter batchEntryNotificationCounter;
    private final DistributionSummary batchSizeSummary;

    /**
     * Creates a WebMVC observer that records transport and batch metrics.
     *
     * @param meterRegistry           registry where metrics are published
     * @param latencyHistogramEnabled whether histogram distribution is enabled for batch sizes
     * @param latencyPercentiles      configured percentiles for batch size distribution
     */
    public JsonRpcWebMvcMetricsObserver(
        MeterRegistry meterRegistry,
        boolean latencyHistogramEnabled,
        double[] latencyPercentiles
    ) {
        MeterRegistry targetRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        double[] configuredPercentiles = Objects.requireNonNull(latencyPercentiles, "latencyPercentiles");

        this.parseErrorCounter = targetRegistry.counter(TRANSPORT_ERRORS_METRIC, "reason", "parse_error");
        this.requestTooLargeCounter = targetRegistry.counter(TRANSPORT_ERRORS_METRIC, "reason", "request_too_large");
        this.singleNotificationCounter = targetRegistry.counter(NOTIFICATION_METRIC, "mode", "single");
        this.batchNotificationCounter = targetRegistry.counter(NOTIFICATION_METRIC, "mode", "batch");
        this.batchRequestAllSuccessCounter = targetRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "all_success");
        this.batchRequestAllErrorCounter = targetRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "all_error");
        this.batchRequestMixedCounter = targetRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "mixed");
        this.batchRequestNotificationOnlyCounter = targetRegistry.counter(
            BATCH_REQUEST_METRIC,
            "outcome",
            "notification_only"
        );
        this.batchEntrySuccessCounter = targetRegistry.counter(BATCH_ENTRY_METRIC, "outcome", "success");
        this.batchEntryErrorCounter = targetRegistry.counter(BATCH_ENTRY_METRIC, "outcome", "error");
        this.batchEntryNotificationCounter = targetRegistry.counter(BATCH_ENTRY_METRIC, "outcome", "notification");

        DistributionSummary.Builder summaryBuilder = DistributionSummary.builder(BATCH_SIZE_METRIC);
        if (latencyHistogramEnabled) {
            summaryBuilder.publishPercentileHistogram();
        }
        if (configuredPercentiles.length > 0) {
            summaryBuilder.publishPercentiles(configuredPercentiles);
        }
        this.batchSizeSummary = summaryBuilder.register(targetRegistry);
    }

    /**
     * Increments parse error counter.
     */
    @Override
    public void onParseError() {
        parseErrorCounter.increment();
    }

    /**
     * Increments oversized request counter.
     *
     * @param actualBytes actual request payload size in bytes
     * @param maxBytes    configured maximum payload size in bytes
     */
    @Override
    public void onRequestTooLarge(int actualBytes, int maxBytes) {
        requestTooLargeCounter.increment();
    }

    /**
     * Records batch composition metrics for success, error, and notification outcomes.
     *
     * @param requestCount number of entries in the incoming batch payload
     * @param responses    emitted JSON-RPC responses for that batch
     */
    @Override
    public void onBatchResponse(int requestCount, List<JsonRpcResponse> responses) {
        int responseCount = responses.size();
        int notificationCount = Math.max(0, requestCount - responseCount);

        int successCount = 0;
        int errorCount = 0;
        for (JsonRpcResponse response : responses) {
            if (response.error() == null) {
                successCount++;
                continue;
            }
            errorCount++;
        }

        batchSizeSummary.record(requestCount);
        incrementByOutcome("success", successCount);
        incrementByOutcome("error", errorCount);
        incrementByOutcome("notification", notificationCount);

        if (responseCount == 0) {
            batchRequestNotificationOnlyCounter.increment();
        } else if (errorCount == 0) {
            batchRequestAllSuccessCounter.increment();
        } else if (successCount == 0) {
            batchRequestAllErrorCounter.increment();
        } else {
            batchRequestMixedCounter.increment();
        }
    }

    /**
     * Records notification-only request handling counts.
     *
     * @param batch        {@code true} if the original payload was a batch array
     * @param requestCount number of request entries in the payload
     */
    @Override
    public void onNotificationOnly(boolean batch, int requestCount) {
        if (batch) {
            batchNotificationCounter.increment();
            return;
        }
        singleNotificationCounter.increment();
    }

    /**
     * Increments batch entry counters by outcome type.
     *
     * @param outcome outcome label ({@code success}, {@code error}, or {@code notification})
     * @param count   number of entries to increment
     */
    private void incrementByOutcome(String outcome, int count) {
        if (count <= 0) {
            return;
        }
        if ("success".equals(outcome)) {
            batchEntrySuccessCounter.increment(count);
            return;
        }
        if ("error".equals(outcome)) {
            batchEntryErrorCounter.increment(count);
            return;
        }
        batchEntryNotificationCounter.increment(count);
    }
}
