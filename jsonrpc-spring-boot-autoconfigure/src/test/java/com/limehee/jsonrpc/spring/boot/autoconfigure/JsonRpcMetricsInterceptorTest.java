package com.limehee.jsonrpc.spring.boot.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.IntNode;

class JsonRpcMetricsInterceptorTest {

    @Test
    void classifiesMethodNotFoundAsResolutionForNonAccessControlThrowable() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcMetricsInterceptor interceptor = new JsonRpcMetricsInterceptor(meterRegistry);
        JsonRpcRequest request = request("lookup");

        interceptor.beforeInvoke(request);
        interceptor.onError(request, new IllegalStateException("missing"),
            JsonRpcError.of(JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found"));

        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.stage.events",
            "method", "lookup",
            "stage", "method_not_found"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.failures",
            "method", "lookup",
            "errorCode", "-32601",
            "source", "resolution"
        ).count());
    }

    @Test
    void classifiesInternalErrorAsHandlerForNonInterceptorThrowable() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcMetricsInterceptor interceptor = new JsonRpcMetricsInterceptor(meterRegistry);
        JsonRpcRequest request = request("explode");

        interceptor.beforeInvoke(request);
        interceptor.onError(request, new IllegalStateException("boom"),
            JsonRpcError.of(JsonRpcErrorCode.INTERNAL_ERROR, "Internal error"));

        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.stage.events",
            "method", "explode",
            "stage", "internal_error"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.failures",
            "method", "explode",
            "errorCode", "-32603",
            "source", "handler"
        ).count());
    }

    @Test
    void classifiesCustomCodeAsCustomStageAndSource() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcMetricsInterceptor interceptor = new JsonRpcMetricsInterceptor(meterRegistry);
        JsonRpcRequest request = request("domain.error");

        interceptor.beforeInvoke(request);
        interceptor.onError(request, new IllegalArgumentException("domain"),
            JsonRpcError.of(-32010, "Domain error"));

        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.stage.events",
            "method", "domain.error",
            "stage", "custom_error"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.failures",
            "method", "domain.error",
            "errorCode", "-32010",
            "source", "custom"
        ).count());
    }

    @Test
    void usesUnknownMethodTagWhenRequestIsMissing() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcMetricsInterceptor interceptor = new JsonRpcMetricsInterceptor(meterRegistry);

        interceptor.onError(null, new IllegalStateException("boom"),
            JsonRpcError.of(JsonRpcErrorCode.INTERNAL_ERROR, "Internal error"));

        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.calls",
            "method", "unknown",
            "outcome", "error",
            "errorCode", "-32603"
        ).count());
    }

    @Test
    void rejectsNonPositiveMaxMethodTagValues() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        assertThrows(
            IllegalArgumentException.class,
            () -> new JsonRpcMetricsInterceptor(meterRegistry, false, new double[0], 0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new JsonRpcMetricsInterceptor(meterRegistry, false, new double[0], -1)
        );
    }

    @Test
    void enforcesHardCapForDistinctMethodTagsUnderConcurrency() throws Exception {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcMetricsInterceptor interceptor = new JsonRpcMetricsInterceptor(meterRegistry, false, new double[0], 1);
        int workers = 64;
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(workers);
        for (int i = 0; i < workers; i++) {
            String methodName = "method." + i;
            futures.add(executor.submit(() -> {
                start.await();
                JsonRpcRequest request = request(methodName);
                interceptor.beforeInvoke(request);
                interceptor.afterInvoke(request, IntNode.valueOf(1));
                return null;
            }));
        }

        start.countDown();
        for (Future<?> future : futures) {
            future.get();
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        long concreteMethodTags = meterRegistry.getMeters().stream()
            .filter(meter -> "jsonrpc.server.calls".equals(meter.getId().getName()))
            .map(meter -> meter.getId().getTag("method"))
            .filter(method -> method != null && !"other".equals(method) && !"unknown".equals(method))
            .distinct()
            .count();

        assertEquals(1, concreteMethodTags);
        assertEquals(63, meterRegistry.counter(
            "jsonrpc.server.calls",
            "method", "other",
            "outcome", "success",
            "errorCode", "none"
        ).count(), 0.0d);
    }

    private JsonRpcRequest request(String method) {
        return new JsonRpcRequest("2.0", IntNode.valueOf(1), method, null, true);
    }
}
