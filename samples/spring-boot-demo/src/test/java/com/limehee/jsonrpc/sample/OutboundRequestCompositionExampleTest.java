package com.limehee.jsonrpc.sample;

import com.limehee.jsonrpc.core.JsonRpcError;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundRequestCompositionExampleTest {

    @Test
    void buildsSingleRequestPayloadForUpstreamCall() {
        ObjectNode request = OutboundRequestCompositionExample.buildInventoryLookupRequest();

        assertEquals("2.0", request.get("jsonrpc").stringValue());
        assertEquals("inventory.lookup", request.get("method").stringValue());
        assertEquals("req-21", request.get("id").stringValue());
        assertEquals("book-001", request.get("params").get("sku").stringValue());
        assertEquals("spring", request.get("params").get("warehouse").stringValue());
    }

    @Test
    void buildsBatchPayloadContainingRequestAndNotification() {
        ArrayNode batch = OutboundRequestCompositionExample.buildInventoryAuditBatch();

        assertEquals(2, batch.size());
        assertEquals("inventory.lookup", batch.get(0).get("method").stringValue());
        assertEquals(21L, batch.get(0).get("id").longValue());
        assertEquals("audit.record", batch.get(1).get("method").stringValue());
        assertFalse(batch.get(1).has("id"));
        assertEquals("spring-boot-demo", batch.get(1).get("params").get("source").stringValue());
    }

    @Test
    void createsStructuredErrorObjectForUpstreamFailure() {
        JsonRpcError error = OutboundRequestCompositionExample.buildUpstreamFailureError();

        assertEquals(-32001, error.code());
        assertEquals("Inventory upstream failed", error.message());
        assertTrue(error.data().isObject());
        assertEquals("trace-spring-123", error.data().get("traceId").stringValue());
        assertEquals("inventory-gateway", error.data().get("service").stringValue());
    }
}
