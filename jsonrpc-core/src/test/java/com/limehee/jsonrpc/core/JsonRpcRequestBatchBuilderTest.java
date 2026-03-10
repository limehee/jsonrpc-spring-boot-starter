package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
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
    void addRequestRejectsNullOrInvalidMethod() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder();

        assertThrows(NullPointerException.class, () -> batchBuilder.addRequest(null, request -> request.id(1L)));
        assertThrows(IllegalArgumentException.class, () -> batchBuilder.addRequest(" ", request -> request.id(1L)));
        assertThrows(IllegalArgumentException.class, () -> batchBuilder.addRequest("rpc.system", request -> request.id(1L)));
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
    void addNotificationRejectsNullOrInvalidMethod() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder();

        assertThrows(NullPointerException.class, () -> batchBuilder.addNotification(null, request -> { }));
        assertThrows(IllegalArgumentException.class, () -> batchBuilder.addNotification(" ", request -> { }));
        assertThrows(IllegalArgumentException.class, () -> batchBuilder.addNotification("rpc.system", request -> { }));
    }

    @Test
    void buildBatchNodePropagatesInvalidRequestBuilderState() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder()
            .add(JsonRpcRequestBuilder.request("ping"));

        assertThrows(IllegalStateException.class, batchBuilder::buildNode);
    }

    @Test
    void buildBatchNodeReturnsDetachedPayloadAcrossBuilderReuse() {
        JsonRpcRequestBatchBuilder batchBuilder = new JsonRpcRequestBatchBuilder()
            .addRequest("state.read", request -> request
                .id(1L)
                .paramsObject(params -> params.put("status", "initial")));

        ArrayNode first = batchBuilder.buildNode();
        ((ObjectNode) first.get(0).get("params")).put("status", "mutated");

        ArrayNode second = batchBuilder.buildNode();

        assertEquals("initial", second.get(0).get("params").get("status").stringValue());
        assertNotSame(first, second);
        assertNotSame(first.get(0), second.get(0));
        assertNotSame(first.get(0).get("params"), second.get(0).get("params"));
    }

    @Test
    void addNotificationCanBuildObjectParams() {
        var batch = new JsonRpcRequestBatchBuilder()
            .addNotification("audit.record", request -> request.paramsObject(params -> {
                params.put("event", "created");
                params.put("source", "gateway");
            }))
            .buildNode();

        ObjectNode request = (ObjectNode) batch.get(0);
        assertFalse(request.has("id"));
        assertEquals("created", request.get("params").get("event").stringValue());
        assertEquals("gateway", request.get("params").get("source").stringValue());
    }
}
