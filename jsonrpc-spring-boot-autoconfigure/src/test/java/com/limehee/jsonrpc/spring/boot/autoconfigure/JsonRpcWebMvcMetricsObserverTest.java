package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcWebMvcMetricsObserver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonRpcWebMvcMetricsObserverTest {

    @Test
    void recordsTransportAndBatchMetrics() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JsonRpcWebMvcMetricsObserver observer = new JsonRpcWebMvcMetricsObserver(
                meterRegistry,
                false,
                new double[0]
        );

        observer.onParseError();
        observer.onRequestTooLarge(2048, 1024);
        observer.onNotificationOnly(false, 1);
        observer.onNotificationOnly(true, 2);
        observer.onBatchResponse(3, List.of(
                JsonRpcResponse.success(IntNode.valueOf(1), TextNode.valueOf("ok")),
                JsonRpcResponse.error(IntNode.valueOf(2), -32601, "Method not found")
        ));

        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.transport.errors",
                "reason", "parse_error"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.transport.errors",
                "reason", "request_too_large"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.transport.notifications",
                "mode", "single"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.transport.notifications",
                "mode", "batch"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.batch.requests",
                "outcome", "mixed"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.batch.entries",
                "outcome", "success"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.batch.entries",
                "outcome", "error"
        ).count());
        assertEquals(1.0, meterRegistry.counter(
                "jsonrpc.server.batch.entries",
                "outcome", "notification"
        ).count());
        assertEquals(1L, meterRegistry.summary("jsonrpc.server.batch.size").count());
        assertEquals(3.0, meterRegistry.summary("jsonrpc.server.batch.size").totalAmount());
    }
}
