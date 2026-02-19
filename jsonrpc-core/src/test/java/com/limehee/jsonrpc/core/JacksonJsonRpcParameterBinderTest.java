package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JacksonJsonRpcParameterBinderTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final JacksonJsonRpcParameterBinder binder = new JacksonJsonRpcParameterBinder(OBJECT_MAPPER);

    @Test
    void bindRejectsNullTargetType() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> binder.bind(null, null));

        assertEquals("targetType must not be null", ex.getMessage());
    }

    @Test
    void bindReturnsJsonNodeWithoutConversion() {
        StringNode node = StringNode.valueOf("value");

        assertSame(node, binder.bind(node, JsonNode.class));
        assertSame(NullNode.getInstance(), binder.bind(NullNode.getInstance(), JsonNode.class));
    }

    @Test
    void bindConvertsObjectNodeToPojo() throws Exception {
        PingParams params = binder.bind(OBJECT_MAPPER.readTree("{\"name\":\"developer\"}"), PingParams.class);

        assertEquals("developer", params.name());
    }

    @Test
    void bindConvertsNullToReferenceType() {
        String value = binder.bind(null, String.class);
        assertNull(value);
    }

    @Test
    void bindThrowsInvalidParamsWhenConversionFails() {
        JsonRpcException ex = assertThrows(JsonRpcException.class,
                () -> binder.bind(StringNode.valueOf("bad"), PingParams.class));

        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
        assertEquals(JsonRpcConstants.MESSAGE_INVALID_PARAMS, ex.getMessage());
    }

    record PingParams(String name) {
    }
}
