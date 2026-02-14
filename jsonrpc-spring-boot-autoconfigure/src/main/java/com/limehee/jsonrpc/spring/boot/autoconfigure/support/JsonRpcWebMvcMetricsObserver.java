package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.List;

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

    public JsonRpcWebMvcMetricsObserver(
            MeterRegistry meterRegistry,
            boolean latencyHistogramEnabled,
            double[] latencyPercentiles
    ) {
        this.parseErrorCounter = meterRegistry.counter(TRANSPORT_ERRORS_METRIC, "reason", "parse_error");
        this.requestTooLargeCounter = meterRegistry.counter(TRANSPORT_ERRORS_METRIC, "reason", "request_too_large");
        this.singleNotificationCounter = meterRegistry.counter(NOTIFICATION_METRIC, "mode", "single");
        this.batchNotificationCounter = meterRegistry.counter(NOTIFICATION_METRIC, "mode", "batch");
        this.batchRequestAllSuccessCounter = meterRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "all_success");
        this.batchRequestAllErrorCounter = meterRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "all_error");
        this.batchRequestMixedCounter = meterRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "mixed");
        this.batchRequestNotificationOnlyCounter = meterRegistry.counter(BATCH_REQUEST_METRIC, "outcome", "notification_only");
        this.batchEntrySuccessCounter = meterRegistry.counter(BATCH_ENTRY_METRIC, "outcome", "success");
        this.batchEntryErrorCounter = meterRegistry.counter(BATCH_ENTRY_METRIC, "outcome", "error");
        this.batchEntryNotificationCounter = meterRegistry.counter(BATCH_ENTRY_METRIC, "outcome", "notification");

        DistributionSummary.Builder summaryBuilder = DistributionSummary.builder(BATCH_SIZE_METRIC);
        if (latencyHistogramEnabled) {
            summaryBuilder.publishPercentileHistogram();
        }
        if (latencyPercentiles != null && latencyPercentiles.length > 0) {
            summaryBuilder.publishPercentiles(latencyPercentiles);
        }
        this.batchSizeSummary = summaryBuilder.register(meterRegistry);
    }

    @Override
    public void onParseError() {
        parseErrorCounter.increment();
    }

    @Override
    public void onRequestTooLarge(int actualBytes, int maxBytes) {
        requestTooLargeCounter.increment();
    }

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

    @Override
    public void onNotificationOnly(boolean batch, int requestCount) {
        if (batch) {
            batchNotificationCounter.increment();
            return;
        }
        singleNotificationCounter.increment();
    }

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
