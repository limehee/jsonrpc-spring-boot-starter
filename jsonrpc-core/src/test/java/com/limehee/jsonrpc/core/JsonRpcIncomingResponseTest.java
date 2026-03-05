package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.IntNode;

class JsonRpcIncomingResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void convenienceConstructorCreatesResponseWithoutSourceNode() {
        JsonRpcIncomingResponse response = new JsonRpcIncomingResponse(
            "2.0",
            IntNode.valueOf(1),
            true,
            IntNode.valueOf(7),
            true,
            null,
            false
        );

        assertNull(response.source());
        assertTrue(response.idPresent());
        assertTrue(response.resultPresent());
        assertFalse(response.errorPresent());
    }

    @Test
    void canonicalConstructorPreservesProvidedSourceNode() {
        var source = OBJECT_MAPPER.createObjectNode().put("id", 1);
        JsonRpcIncomingResponse response = new JsonRpcIncomingResponse(
            source,
            "2.0",
            IntNode.valueOf(1),
            true,
            IntNode.valueOf(7),
            true,
            null,
            false
        );

        assertEquals(1, response.source().get("id").asInt());
    }
}
