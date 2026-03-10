package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

class JsonRpcRequestBatchBuilderTest {

    @Test
    void buildBatchNodeWithRequestAndNotificationEntries() {
        var batch = new JsonRpcRequestBatchBuilder()
            .add(JsonRpcRequestBuilder.request("ping").id(1L))
            .add(JsonRpcRequestBuilder.notification("notify.mark"))
            .buildNode();

        assertEquals(2, batch.size());
        assertEquals("ping", batch.get(0).get("method").stringValue());
        assertEquals(1L, batch.get(0).get("id").longValue());
        assertEquals("notify.mark", batch.get(1).get("method").stringValue());
        assertFalse(batch.get(1).has("id"));
    }

    @Test
    void buildBatchNodeRejectsEmptyBatch() {
        assertThrows(
            IllegalStateException.class,
            () -> new JsonRpcRequestBatchBuilder().buildNode()
        );
    }

    @Test
    void addRejectsNullBuilder() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder();

        assertThrows(NullPointerException.class, () -> batchBuilder.add(null));
    }

    @Test
    void addRequestAppliesCustomizer() {
        var batch = new JsonRpcRequestBatchBuilder()
            .addRequest("sum", request -> request
                .id(10L)
                .paramsObject(params -> {
                    params.put("left", 4);
                    params.put("right", 6);
                }))
            .buildNode();

        ObjectNode request = (ObjectNode) batch.get(0);
        assertEquals(10L, request.get("id").longValue());
        assertEquals(4, request.get("params").get("left").intValue());
        assertEquals(6, request.get("params").get("right").intValue());
    }

    @Test
    void addRequestRejectsNullCustomizer() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder();

        assertThrows(NullPointerException.class, () -> batchBuilder.addRequest("sum", null));
    }

    @Test
    void addNotificationAppliesCustomizer() {
        var batch = new JsonRpcRequestBatchBuilder()
            .addNotification("typed.tags", request -> request.paramsArray(IntNode.valueOf(1)))
            .buildNode();

        ObjectNode request = (ObjectNode) batch.get(0);
        assertEquals("typed.tags", request.get("method").stringValue());
        assertFalse(request.has("id"));
        assertEquals(1, request.get("params").get(0).intValue());
    }

    @Test
    void addNotificationRejectsNullCustomizer() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder();

        assertThrows(NullPointerException.class, () -> batchBuilder.addNotification("typed.tags", null));
    }

    @Test
    void buildBatchNodePropagatesInvalidRequestBuilderState() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder()
            .add(JsonRpcRequestBuilder.request("ping"));

        assertThrows(IllegalStateException.class, batchBuilder::buildNode);
    }
}
