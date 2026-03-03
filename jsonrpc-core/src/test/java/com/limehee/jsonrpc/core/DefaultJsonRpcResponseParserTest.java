package com.limehee.jsonrpc.core;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultJsonRpcResponseParserTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final JsonRpcResponseParser parser = new DefaultJsonRpcResponseParser();

    @Test
    void parseParsesSingleResponseObject() throws Exception {
        JsonRpcIncomingResponseEnvelope envelope = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","id":1,"result":{"ok":true}}
                """));

        assertFalse(envelope.isBatch());
        JsonRpcIncomingResponse response = envelope.singleResponse().orElseThrow();
        assertEquals("2.0", response.jsonrpc());
        assertTrue(response.idPresent());
        assertTrue(response.resultPresent());
        assertFalse(response.errorPresent());
    }

    @Test
    void parseParsesBatchResponseArray() throws Exception {
        JsonRpcIncomingResponseEnvelope envelope = parser.parse(OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","id":"a","result":1},
                  {"jsonrpc":"2.0","id":"b","error":{"code":-32000,"message":"x"}}
                ]
                """));

        assertTrue(envelope.isBatch());
        assertEquals(2, envelope.responses().size());
    }

    @Test
    void parseStoresNullVersionWhenJsonrpcFieldIsNotString() throws Exception {
        JsonRpcIncomingResponseEnvelope envelope = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":2,"id":1,"result":true}
                """));

        JsonRpcIncomingResponse response = envelope.singleResponse().orElseThrow();
        assertNull(response.jsonrpc());
    }

    @Test
    void parsePreservesFieldPresenceForNullValues() throws Exception {
        JsonRpcIncomingResponseEnvelope envelope = parser.parse(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","id":null,"result":null}
                """));

        JsonRpcIncomingResponse response = envelope.singleResponse().orElseThrow();
        assertTrue(response.idPresent());
        assertTrue(response.resultPresent());
        assertFalse(response.errorPresent());
    }

    @Test
    void parseRejectsNullPayload() {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> parser.parse(null));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void parseRejectsPrimitiveOrEmptyArrayOrNonObjectBatchEntries() throws Exception {
        assertThrows(JsonRpcException.class, () -> parser.parse(OBJECT_MAPPER.readTree("1")));
        assertThrows(JsonRpcException.class, () -> parser.parse(OBJECT_MAPPER.readTree("[]")));
        assertThrows(JsonRpcException.class, () -> parser.parse(OBJECT_MAPPER.readTree("""
                [{"jsonrpc":"2.0","id":1,"result":1}, 2]
                """)));
    }
}
