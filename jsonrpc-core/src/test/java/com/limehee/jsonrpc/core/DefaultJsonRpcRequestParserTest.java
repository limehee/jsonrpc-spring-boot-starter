package com.limehee.jsonrpc.core;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultJsonRpcRequestParserTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final DefaultJsonRpcRequestParser parser = new DefaultJsonRpcRequestParser();

    @Test
    void parseRejectsNullNode() {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> parser.parse(null));

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
        assertEquals(JsonRpcConstants.MESSAGE_INVALID_REQUEST, ex.getMessage());
    }

    @Test
    void parseRejectsNonObjectPayload() throws Exception {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> parser.parse(OBJECT_MAPPER.readTree("[]")));

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void parseExtractsRequestFields() throws Exception {
        JsonRpcRequest request = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","params":{"value":1},"id":"abc"}
                """));

        assertEquals("2.0", request.jsonrpc());
        assertEquals("ping", request.method());
        assertEquals(1, request.params().get("value").asInt());
        assertEquals("abc", request.id().asText());
        assertTrue(request.idPresent());
        assertFalse(request.isNotification());
    }

    @Test
    void parseTreatsMissingIdAsNotification() throws Exception {
        JsonRpcRequest request = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping"}
                """));

        assertFalse(request.idPresent());
        assertNull(request.id());
        assertTrue(request.isNotification());
    }

    @Test
    void parseDistinguishesExplicitNullIdFromAbsentId() throws Exception {
        JsonRpcRequest request = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":null}
                """));

        assertTrue(request.idPresent());
        assertTrue(request.id().isNull());
        assertFalse(request.isNotification());
    }

    @Test
    void parseConvertsNonTextJsonrpcAndMethodToNull() throws Exception {
        JsonRpcRequest request = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":2.0,"method":true,"id":1}
                """));

        assertNull(request.jsonrpc());
        assertNull(request.method());
        assertEquals(1, request.id().asInt());
    }
}
