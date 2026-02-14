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

    private final MeterRegistry meterRegistry;
    private final DistributionSummary batchSizeSummary;

    public JsonRpcWebMvcMetricsObserver(
            MeterRegistry meterRegistry,
            boolean latencyHistogramEnabled,
            double[] latencyPercentiles
    ) {
        this.meterRegistry = meterRegistry;
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
        meterRegistry.counter(TRANSPORT_ERRORS_METRIC, "reason", "parse_error").increment();
    }

    @Override
    public void onRequestTooLarge(int actualBytes, int maxBytes) {
        meterRegistry.counter(TRANSPORT_ERRORS_METRIC, "reason", "request_too_large").increment();
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

        String batchOutcome;
        if (responseCount == 0) {
            batchOutcome = "notification_only";
        } else if (errorCount == 0) {
            batchOutcome = "all_success";
        } else if (successCount == 0) {
            batchOutcome = "all_error";
        } else {
            batchOutcome = "mixed";
        }
        meterRegistry.counter(BATCH_REQUEST_METRIC, "outcome", batchOutcome).increment();
    }

    @Override
    public void onNotificationOnly(boolean batch, int requestCount) {
        Counter counter = meterRegistry.counter(
                NOTIFICATION_METRIC,
                "mode", batch ? "batch" : "single"
        );
        counter.increment();
    }

    private void incrementByOutcome(String outcome, int count) {
        if (count <= 0) {
            return;
        }
        meterRegistry.counter(BATCH_ENTRY_METRIC, "outcome", outcome).increment(count);
    }
}
