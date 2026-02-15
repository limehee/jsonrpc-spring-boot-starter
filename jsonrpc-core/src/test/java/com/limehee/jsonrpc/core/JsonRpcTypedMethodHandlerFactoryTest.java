package com.limehee.jsonrpc.core;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonRpcTypedMethodHandlerFactoryTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final JsonRpcTypedMethodHandlerFactory factory = new DefaultJsonRpcTypedMethodHandlerFactory(
            new JacksonJsonRpcParameterBinder(OBJECT_MAPPER),
            new JacksonJsonRpcResultWriter(OBJECT_MAPPER)
    );

    @Test
    void noParamsBindsAndWritesResult() {
        JsonRpcMethodHandler handler = factory.noParams(() -> "pong");

        assertEquals("pong", handler.handle(null).asText());
        assertEquals("pong", handler.handle(OBJECT_MAPPER.createObjectNode()).asText());
    }

    @Test
    void noParamsRejectsUnexpectedParams() {
        JsonRpcMethodHandler handler = factory.noParams(() -> "pong");

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> handler.handle(StringNode.valueOf("bad")));
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
    }

    @Test
    void unaryBindsObjectParams() throws Exception {
        JsonRpcMethodHandler handler = factory.unary(PingParams.class, params -> "hello " + params.name());

        assertEquals("hello developer", handler.handle(OBJECT_MAPPER.readTree("{\"name\":\"developer\"}")).asText());
    }

    @Test
    void unaryBindingFailureThrowsInvalidParams() {
        JsonRpcMethodHandler handler = factory.unary(PingParams.class, params -> "hello " + params.name());

        JsonRpcException ex = assertThrows(JsonRpcException.class,
                () -> handler.handle(StringNode.valueOf("bad")));
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
        assertEquals(JsonRpcConstants.MESSAGE_INVALID_PARAMS, ex.getMessage());
    }

    record PingParams(String name) {
    }
}
