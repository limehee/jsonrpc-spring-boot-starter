package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcNotificationExecutor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Decorator that records Micrometer metrics for notification task execution.
 * <p>
 * The wrapper captures:
 * </p>
 * <ul>
 * <li>submission count</li>
 * <li>queue delay between submission and execution start</li>
 * <li>execution duration</li>
 * <li>execution failures</li>
 * </ul>
 */
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

    /**
     * Creates an instrumented notification executor.
     *
     * @param delegate                delegate executor that performs actual task scheduling and execution
     * @param meterRegistry           registry where notification metrics are emitted
     * @param latencyHistogramEnabled whether percentile histograms are enabled for timers
     * @param latencyPercentiles      configured percentiles for queue/execution timers
     */
    public InstrumentedJsonRpcNotificationExecutor(
        JsonRpcNotificationExecutor delegate,
        MeterRegistry meterRegistry,
        boolean latencyHistogramEnabled,
        double[] latencyPercentiles
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        MeterRegistry targetRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        double[] configuredPercentiles = Objects.requireNonNull(latencyPercentiles, "latencyPercentiles");
        this.queueDelayTimer = createTimer(
            targetRegistry,
            QUEUE_DELAY_METRIC,
            latencyHistogramEnabled,
            configuredPercentiles
        );
        this.executionTimer = createTimer(
            targetRegistry,
            EXECUTION_METRIC,
            latencyHistogramEnabled,
            configuredPercentiles
        );
        this.submittedCounter = targetRegistry.counter(SUBMITTED_METRIC);
        this.failedCounter = targetRegistry.counter(FAILED_METRIC);
    }

    /**
     * Submits a notification task and records queue, duration, and failure metrics.
     *
     * @param task notification task to execute
     */
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

    /**
     * Creates a timer using shared histogram/percentile settings.
     *
     * @param meterRegistry           registry where the timer is registered
     * @param name                    metric name
     * @param latencyHistogramEnabled whether histogram publication is enabled
     * @param latencyPercentiles      percentile configuration to publish
     * @return registered timer instance
     */
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
        if (latencyPercentiles.length > 0) {
            builder.publishPercentiles(latencyPercentiles);
        }
        return builder.register(meterRegistry);
    }
}
