package com.limehee.jsonrpc.spring.boot.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.StringNode;

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
    void doesNotCollapseMethodTagsWhenCardinalityLimitIsDisabled() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcMetricsInterceptor interceptor = new JsonRpcMetricsInterceptor(
            meterRegistry,
            false,
            new double[0],
            0
        );
        JsonRpcRequest first = request("method.a");
        JsonRpcRequest second = request("method.b");

        interceptor.beforeInvoke(first);
        interceptor.afterInvoke(first, StringNode.valueOf("a"));

        interceptor.beforeInvoke(second);
        interceptor.afterInvoke(second, StringNode.valueOf("b"));

        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.calls",
            "method", "method.a",
            "outcome", "success",
            "errorCode", "none"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
            "jsonrpc.server.calls",
            "method", "method.b",
            "outcome", "success",
            "errorCode", "none"
        ).count());
    }

    private JsonRpcRequest request(String method) {
        return new JsonRpcRequest("2.0", IntNode.valueOf(1), method, null, true);
    }
}
