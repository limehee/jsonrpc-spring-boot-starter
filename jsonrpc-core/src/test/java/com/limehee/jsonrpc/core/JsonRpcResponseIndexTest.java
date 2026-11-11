package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

class JsonRpcResponseIndexTest {

    private static final DefaultJsonRpcResponseParser PARSER = new DefaultJsonRpcResponseParser();
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private static JsonRpcResponseIndex index(String json) {
        return JsonRpcResponseIndex.of(PARSER.parse(json));
    }

    @Test
    void emptyBatchHasNothingIndexed() {
        JsonRpcResponseIndex index = JsonRpcResponseIndex.of(JsonRpcIncomingResponseEnvelope.batch(List.of()));

        assertEquals(0, index.size());
        assertTrue(index.ids().isEmpty());
        assertTrue(index.successes().isEmpty());
        assertTrue(index.errors().isEmpty());
        assertTrue(index.unidentified().isEmpty());
        assertFalse(index.hasDuplicateIds());
        assertTrue(index.findById(1).isEmpty());
        assertThrows(NoSuchElementException.class, () -> index.requireById("a"));
    }

    @Test
    void correlatesOutOfOrderMixedBatch() {
        JsonRpcResponseIndex index = index("""
            [
              {"jsonrpc":"2.0","id":"b","error":{"code":-32601,"message":"Method not found"}},
              {"jsonrpc":"2.0","id":2,"result":"two"},
              {"jsonrpc":"2.0","id":1,"result":"one"}
            ]
            """);

        assertEquals(3, index.size());
        assertEquals("one", index.requireById(1).result().asString());
        assertEquals("two", index.requireById(2).result().asString());
        assertEquals(-32601, index.requireById("b").error().get("code").asInt());
        assertEquals(List.of(NODES.stringNode("b"), NODES.numberNode(2), NODES.numberNode(1)), index.ids());

        assertEquals(2, index.successes().size());
        assertSame(index.requireById(2), index.successes().get(0));
        assertEquals(1, index.errors().size());
        assertSame(index.requireById("b"), index.errors().get(0));
    }

    @Test
    void indexesSingleResponse() {
        JsonRpcResponseIndex index = index("""
            {"jsonrpc":"2.0","id":7,"result":true}
            """);

        assertEquals(1, index.size());
        assertTrue(index.requireById(7).result().asBoolean());
    }

    @Test
    void matchesNumericIdsByValueAndKeepsStringsDistinct() {
        JsonRpcResponseIndex index = index("""
            [
              {"jsonrpc":"2.0","id":1.0,"result":"numeric"},
              {"jsonrpc":"2.0","id":"1","result":"text"},
              {"jsonrpc":"2.0","id":-0.0,"result":"zero"}
            ]
            """);

        assertEquals("numeric", index.requireById(1).result().asString());
        assertEquals("numeric", index.requireById(NODES.numberNode(1L)).result().asString());
        assertEquals("text", index.requireById("1").result().asString());
        assertEquals("zero", index.requireById(0).result().asString());
        assertFalse(index.hasDuplicateIds());
    }

    @Test
    void separatesUncorrelatableResponses() {
        JsonRpcResponseIndex index = index("""
            [
              {"jsonrpc":"2.0","id":null,"error":{"code":-32700,"message":"Parse error"}},
              {"jsonrpc":"2.0","result":"no id"},
              {"jsonrpc":"2.0","id":{"x":1},"result":"object id"},
              {"jsonrpc":"2.0","id":1,"result":"ok"}
            ]
            """);

        assertEquals(4, index.size());
        assertEquals(3, index.unidentified().size());
        assertEquals(List.of(NODES.numberNode(1)), index.ids());
        assertEquals(1, index.errors().size());
        assertEquals(3, index.successes().size());
    }

    @Test
    void duplicateIdsAreReportedAndLookupFails() {
        JsonRpcResponseIndex index = index("""
            [
              {"jsonrpc":"2.0","id":1,"result":"first"},
              {"jsonrpc":"2.0","id":2,"result":"two"},
              {"jsonrpc":"2.0","id":1,"result":"second"}
            ]
            """);

        assertTrue(index.hasDuplicateIds());
        assertEquals(List.of(NODES.numberNode(1)), index.duplicateIds());
        assertTrue(index.containsId(NODES.numberNode(1)));
        assertThrows(IllegalStateException.class, () -> index.findById(1));
        assertThrows(IllegalStateException.class, () -> index.requireById(1));
        assertEquals("two", index.requireById(2).result().asString());
        assertEquals(List.of(NODES.numberNode(1), NODES.numberNode(2)), index.ids());
    }

    @Test
    void malformedResultErrorShapesAreInNeitherView() {
        JsonRpcResponseIndex index = index("""
            [
              {"jsonrpc":"2.0","id":1,"result":1,"error":{"code":-32603,"message":"x"}},
              {"jsonrpc":"2.0","id":2}
            ]
            """);

        assertTrue(index.successes().isEmpty());
        assertTrue(index.errors().isEmpty());
        assertEquals(2, index.ids().size());
    }

    @Test
    void rejectsNonScalarLookupIds() {
        JsonRpcResponseIndex index = JsonRpcResponseIndex.of(List.of());

        assertThrows(IllegalArgumentException.class, () -> index.findById(NODES.nullNode()));
        assertThrows(IllegalArgumentException.class, () -> index.containsId(NODES.booleanNode(true)));
        assertThrows(IllegalArgumentException.class,
            () -> index.findById(JsonMapper.builder().build().createObjectNode()));
    }

    @Test
    void rejectsNullArguments() {
        JsonRpcResponseIndex index = JsonRpcResponseIndex.of(List.of());

        assertThrows(NullPointerException.class, () -> JsonRpcResponseIndex.of((JsonRpcIncomingResponseEnvelope) null));
        assertThrows(NullPointerException.class, () -> JsonRpcResponseIndex.of((List<JsonRpcIncomingResponse>) null));
        assertThrows(NullPointerException.class, () -> index.findById((String) null));
        assertThrows(NullPointerException.class, () -> index.requireById((String) null));
    }

    @Test
    void returnedListsAreImmutable() {
        JsonRpcResponseIndex index = index("""
            [{"jsonrpc":"2.0","id":1,"result":1}]
            """);

        assertThrows(UnsupportedOperationException.class, () -> index.responses().clear());
        assertThrows(UnsupportedOperationException.class, () -> index.successes().clear());
        assertThrows(UnsupportedOperationException.class, () -> index.ids().clear());
    }
}
