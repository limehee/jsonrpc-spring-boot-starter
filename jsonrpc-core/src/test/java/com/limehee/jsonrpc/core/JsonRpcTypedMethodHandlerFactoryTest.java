package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonRpcTypedMethodHandlerFactoryTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> handler.handle(TextNode.valueOf("bad")));
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
    }

    @Test
    void unaryBindsObjectParams() throws Exception {
        JsonRpcMethodHandler handler = factory.unary(PingParams.class, params -> "hello " + params.name());

        assertEquals("hello codex", handler.handle(OBJECT_MAPPER.readTree("{\"name\":\"codex\"}")).asText());
    }

    @Test
    void unaryBindingFailureThrowsInvalidParams() {
        JsonRpcMethodHandler handler = factory.unary(PingParams.class, params -> "hello " + params.name());

        JsonRpcException ex = assertThrows(JsonRpcException.class,
                () -> handler.handle(TextNode.valueOf("bad")));
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
        assertEquals(JsonRpcConstants.MESSAGE_INVALID_PARAMS, ex.getMessage());
    }

    record PingParams(String name) {
    }
}
