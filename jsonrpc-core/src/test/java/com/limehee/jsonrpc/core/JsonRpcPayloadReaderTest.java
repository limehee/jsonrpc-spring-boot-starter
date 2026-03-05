package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JsonRpcPayloadReaderTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void readTreeFromStringRejectsDuplicateMembersWhenEnabled() {
        JsonRpcPayloadReader reader = new JsonRpcPayloadReader(OBJECT_MAPPER, true);

        assertThrows(
            JacksonException.class,
            () -> reader.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"id\":2,\"result\":1}")
        );
    }

    @Test
    void readTreeFromStringAcceptsDuplicateMembersWhenDisabled() throws Exception {
        JsonRpcPayloadReader reader = new JsonRpcPayloadReader(OBJECT_MAPPER, false);

        assertEquals(2, reader.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"id\":2,\"result\":1}").get("id").asInt());
    }

    @Test
    void readTreeFromBytesRejectsDuplicateMembersWhenEnabled() {
        JsonRpcPayloadReader reader = new JsonRpcPayloadReader(OBJECT_MAPPER, true);

        assertThrows(
            JacksonException.class,
            () -> reader.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"id\":2,\"result\":1}".getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void readTreeFromBytesAcceptsDuplicateMembersWhenDisabled() throws Exception {
        JsonRpcPayloadReader reader = new JsonRpcPayloadReader(OBJECT_MAPPER, false);

        assertEquals(
            2,
            reader.readTree("{\"jsonrpc\":\"2.0\",\"id\":1,\"id\":2,\"result\":1}".getBytes(StandardCharsets.UTF_8))
                .get("id")
                .asInt()
        );
    }
}

