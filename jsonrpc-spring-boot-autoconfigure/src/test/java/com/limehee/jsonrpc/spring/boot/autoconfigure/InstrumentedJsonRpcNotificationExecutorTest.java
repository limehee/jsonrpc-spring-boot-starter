package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcNotificationExecutor;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.InstrumentedJsonRpcNotificationExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentedJsonRpcNotificationExecutorTest {

    @Test
    void recordsQueueDelayAndExecutionMetrics() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        AtomicInteger executeCount = new AtomicInteger();
        JsonRpcNotificationExecutor delegate = task -> {
            executeCount.incrementAndGet();
            task.run();
        };
        InstrumentedJsonRpcNotificationExecutor executor = new InstrumentedJsonRpcNotificationExecutor(
                delegate,
                meterRegistry,
                false,
                new double[0]
        );

        executor.execute(() -> {
        });

        assertEquals(1, executeCount.get());
        assertEquals(1.0, meterRegistry.counter("jsonrpc.server.notification.submitted").count());
        assertEquals(1L, meterRegistry.timer("jsonrpc.server.notification.queue.delay").count());
        assertEquals(1L, meterRegistry.timer("jsonrpc.server.notification.execution").count());
        assertEquals(0.0, meterRegistry.counter("jsonrpc.server.notification.failed").count());
    }

    @Test
    void recordsFailedNotificationExecution() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcNotificationExecutor delegate = Runnable::run;
        InstrumentedJsonRpcNotificationExecutor executor = new InstrumentedJsonRpcNotificationExecutor(
                delegate,
                meterRegistry,
                false,
                new double[0]
        );

        assertThrows(IllegalStateException.class, () ->
                executor.execute(() -> {
                    throw new IllegalStateException("boom");
                }));

        assertEquals(1.0, meterRegistry.counter("jsonrpc.server.notification.submitted").count());
        assertEquals(1.0, meterRegistry.counter("jsonrpc.server.notification.failed").count());
        assertEquals(1L, meterRegistry.timer("jsonrpc.server.notification.execution").count());
    }
}
