package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

class JsonRpcRequestBuilderTest {

    @Test
    void buildRequestNodeWithObjectParamsAndLongId() {
        ObjectNode node = JsonRpcRequestBuilder.request("sum")
            .id(7L)
            .paramsObject(params -> {
                params.put("left", 2);
                params.put("right", 5);
            })
            .buildNode();

        assertEquals("2.0", node.get("jsonrpc").stringValue());
        assertEquals("sum", node.get("method").stringValue());
        assertEquals(7L, node.get("id").longValue());
        assertEquals(2, node.get("params").get("left").intValue());
        assertEquals(5, node.get("params").get("right").intValue());
    }

    @Test
    void buildNotificationNodeWithoutId() {
        ObjectNode node = JsonRpcRequestBuilder.notification("heartbeat")
            .buildNode();

        assertEquals("2.0", node.get("jsonrpc").stringValue());
        assertEquals("heartbeat", node.get("method").stringValue());
        assertFalse(node.has("id"));
    }

    @Test
    void buildRequestNodeWithExplicitNullId() {
        ObjectNode node = JsonRpcRequestBuilder.request("ping")
            .nullId()
            .buildNode();

        assertTrue(node.has("id"));
        assertTrue(node.get("id").isNull());
    }

    @Test
    void buildRequestNodeWithStringId() {
        ObjectNode node = JsonRpcRequestBuilder.request("lookup")
            .id("request-7")
            .buildNode();

        assertEquals("request-7", node.get("id").stringValue());
    }

    @Test
    void buildRequestNodeWithJsonNodeId() {
        ObjectNode node = JsonRpcRequestBuilder.request("lookup")
            .id(IntNode.valueOf(12))
            .buildNode();

        assertEquals(12, node.get("id").intValue());
    }

    @Test
    void requestBuildNodeRejectsMissingId() {
        assertThrows(
            IllegalStateException.class,
            () -> JsonRpcRequestBuilder.request("ping").buildNode()
        );
    }

    @Test
    void notificationRejectsIdAssignment() {
        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.notification("ping");

        assertThrows(IllegalStateException.class, () -> builder.id(1L));
        assertThrows(IllegalStateException.class, () -> builder.id("request-1"));
        assertThrows(IllegalStateException.class, () -> builder.id(IntNode.valueOf(1)));
        assertThrows(IllegalStateException.class, builder::nullId);
    }

    @Test
    void requestRejectsRepeatedIdAssignment() {
        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.request("ping").id(1L);

        assertThrows(IllegalStateException.class, () -> builder.id(2L));
    }

    @Test
    void requestRejectsRepeatedParamsConfiguration() {
        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.request("ping")
            .id(1L)
            .paramsObject(params -> params.put("value", "first"));

        assertThrows(
            IllegalStateException.class,
            () -> builder.paramsArray(IntNode.valueOf(1))
        );
        assertThrows(
            IllegalStateException.class,
            () -> builder.paramsObject(params -> params.put("value", "second"))
        );
    }

    @Test
    void paramsRejectPrimitiveNode() {
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonRpcRequestBuilder.request("ping").id(1L).params(IntNode.valueOf(3))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonRpcRequestBuilder.request("ping").id(1L).params(NullNode.getInstance())
        );
    }

    @Test
    void paramsAcceptArrayNode() {
        ArrayNode params = JsonNodeFactory.instance.arrayNode()
            .add(1)
            .add(2);

        ObjectNode node = JsonRpcRequestBuilder.request("sum")
            .id(1L)
            .params(params)
            .buildNode();

        assertTrue(node.get("params").isArray());
        assertEquals(1, node.get("params").get(0).intValue());
        assertEquals(2, node.get("params").get(1).intValue());
    }

    @Test
    void paramsSnapshotPreventsOriginalObjectMutationFromLeakingIntoBuiltPayload() {
        ObjectNode params = JsonNodeFactory.instance.objectNode();
        params.put("status", "initial");

        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.request("state.read")
            .id(1L)
            .params(params);

        params.put("status", "mutated");

        ObjectNode node = builder.buildNode();

        assertEquals("initial", node.get("params").get("status").stringValue());
        assertNotSame(params, node.get("params"));
    }

    @Test
    void buildNodeReturnsDetachedParamsSnapshotAcrossBuilderReuse() {
        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.request("state.read")
            .id(1L)
            .paramsObject(params -> params.put("status", "initial"));

        ObjectNode first = builder.buildNode();
        JsonNode firstParams = first.get("params");
        ((ObjectNode) firstParams).put("status", "mutated");

        ObjectNode second = builder.buildNode();

        assertEquals("initial", second.get("params").get("status").stringValue());
        assertNotSame(first.get("params"), second.get("params"));
    }

    @Test
    void idRejectsNonScalarNode() {
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonRpcRequestBuilder.request("ping").id(JsonNodeFactory.instance.objectNode())
        );
    }

    @Test
    void paramsArrayCreatesArrayNodeAndPreservesNullEntries() {
        ObjectNode node = JsonRpcRequestBuilder.request("sum")
            .id(1L)
            .paramsArray(IntNode.valueOf(1), null, IntNode.valueOf(3))
            .buildNode();

        assertTrue(node.get("params").isArray());
        assertEquals(1, node.get("params").get(0).intValue());
        assertTrue(node.get("params").get(1).isNull());
        assertEquals(3, node.get("params").get(2).intValue());
    }

    @Test
    void requestRejectsBlankOrReservedMethodNames() {
        assertThrows(IllegalArgumentException.class, () -> JsonRpcRequestBuilder.request(" "));
        assertThrows(IllegalArgumentException.class, () -> JsonRpcRequestBuilder.request("rpc.system"));
    }
}
