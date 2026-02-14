package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcNotificationExecutor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

public final class InstrumentedJsonRpcNotificationExecutor implements JsonRpcNotificationExecutor {

    private static final String QUEUE_DELAY_METRIC = "jsonrpc.server.notification.queue.delay";
    private static final String EXECUTION_METRIC = "jsonrpc.server.notification.execution";
    private static final String SUBMITTED_METRIC = "jsonrpc.server.notification.submitted";
    private static final String FAILED_METRIC = "jsonrpc.server.notification.failed";

    private final JsonRpcNotificationExecutor delegate;
    private final Timer queueDelayTimer;
    private final Timer executionTimer;
    private final Counter submittedCounter;
    private final Counter failedCounter;

    public InstrumentedJsonRpcNotificationExecutor(
            JsonRpcNotificationExecutor delegate,
            MeterRegistry meterRegistry,
            boolean latencyHistogramEnabled,
            double[] latencyPercentiles
    ) {
        this.delegate = delegate;
        this.queueDelayTimer = createTimer(
                meterRegistry,
                QUEUE_DELAY_METRIC,
                latencyHistogramEnabled,
                latencyPercentiles
        );
        this.executionTimer = createTimer(
                meterRegistry,
                EXECUTION_METRIC,
                latencyHistogramEnabled,
                latencyPercentiles
        );
        this.submittedCounter = meterRegistry.counter(SUBMITTED_METRIC);
        this.failedCounter = meterRegistry.counter(FAILED_METRIC);
    }

    @Override
    public void execute(Runnable task) {
        submittedCounter.increment();
        long queuedAtNanos = System.nanoTime();
        delegate.execute(() -> {
            queueDelayTimer.record(Math.max(0L, System.nanoTime() - queuedAtNanos), TimeUnit.NANOSECONDS);
            long startNanos = System.nanoTime();
            try {
                task.run();
            } catch (Throwable throwable) {
                failedCounter.increment();
                throw throwable;
            } finally {
                executionTimer.record(Math.max(0L, System.nanoTime() - startNanos), TimeUnit.NANOSECONDS);
            }
        });
    }

    private Timer createTimer(
            MeterRegistry meterRegistry,
            String name,
            boolean latencyHistogramEnabled,
            double[] latencyPercentiles
    ) {
        Timer.Builder builder = Timer.builder(name);
        if (latencyHistogramEnabled) {
            builder.publishPercentileHistogram();
        }
        if (latencyPercentiles != null && latencyPercentiles.length > 0) {
            builder.publishPercentiles(latencyPercentiles);
        }
        return builder.register(meterRegistry);
    }
}
