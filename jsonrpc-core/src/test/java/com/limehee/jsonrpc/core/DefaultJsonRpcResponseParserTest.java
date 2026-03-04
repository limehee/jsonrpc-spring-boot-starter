package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class DefaultJsonRpcResponseParserTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final DefaultJsonRpcResponseParser parser = new DefaultJsonRpcResponseParser();

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
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> parser.parse((JsonNode) null));
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

    @Test
    void parseStringParsesSingleResponseObject() {
        JsonRpcIncomingResponseEnvelope envelope = parser.parse("""
            {"jsonrpc":"2.0","id":1,"result":{"ok":true}}
            """);

        assertFalse(envelope.isBatch());
        assertTrue(envelope.singleResponse().isPresent());
    }

    @Test
    void parseStringRejectsMalformedJson() {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> parser.parse("{"));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void parseStringRejectsDuplicateMembersWhenEnabled() {
        DefaultJsonRpcResponseParser strictParser = new DefaultJsonRpcResponseParser(true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> strictParser.parse("""
            {"jsonrpc":"2.0","id":1,"id":2,"result":1}
            """));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void parseStringAllowsDuplicateMembersWhenDisabled() {
        JsonRpcIncomingResponseEnvelope envelope = parser.parse("""
            {"jsonrpc":"2.0","id":1,"id":2,"result":1}
            """);

        JsonRpcIncomingResponse response = envelope.singleResponse().orElseThrow();
        assertEquals(2, response.id().asInt());
    }

    @Test
    void parseBytesRejectsMalformedJson() {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> parser.parse("{".getBytes()));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }
}
