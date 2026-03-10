package com.limehee.jsonrpc.sample.purejava;

import com.limehee.jsonrpc.core.JsonRpcError;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutboundRequestCompositionExampleTest {

    @Test
    void buildsSingleRequestPayloadWithObjectParams() {
        ObjectNode request = OutboundRequestCompositionExample.buildInventoryLookupRequest();

        assertEquals("2.0", request.get("jsonrpc").stringValue());
        assertEquals("inventory.lookup", request.get("method").stringValue());
        assertEquals("req-7", request.get("id").stringValue());
        assertEquals("book-001", request.get("params").get("sku").stringValue());
        assertEquals("seoul", request.get("params").get("warehouse").stringValue());
    }

    @Test
    void buildsNotificationPayloadWithoutId() {
        ObjectNode notification = OutboundRequestCompositionExample.buildAuditNotification();

        assertEquals("audit.record", notification.get("method").stringValue());
        assertFalse(notification.has("id"));
        assertEquals("inventory.lookup", notification.get("params").get("event").stringValue());
        assertEquals("gateway", notification.get("params").get("source").stringValue());
    }

    @Test
    void buildsBatchPayloadContainingRequestAndNotification() {
        ArrayNode batch = OutboundRequestCompositionExample.buildOutboundBatch();

        assertEquals(2, batch.size());
        assertEquals("inventory.lookup", batch.get(0).get("method").stringValue());
        assertEquals(1L, batch.get(0).get("id").longValue());
        assertEquals("audit.record", batch.get(1).get("method").stringValue());
        assertFalse(batch.get(1).has("id"));
    }

    @Test
    void createsErrorObjectWithStructuredData() {
        JsonRpcError error = OutboundRequestCompositionExample.buildUpstreamFailureError();

        assertEquals(-32001, error.code());
        assertEquals("Inventory upstream failed", error.message());
        assertTrue(error.data().isObject());
        assertEquals("trace-123", error.data().get("traceId").stringValue());
        assertEquals("inventory-gateway", error.data().get("service").stringValue());
    }
}
