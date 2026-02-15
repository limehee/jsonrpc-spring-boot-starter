package com.limehee.jsonrpc.core;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultJsonRpcRequestValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final DefaultJsonRpcRequestValidator validator = new DefaultJsonRpcRequestValidator();

    @Test
    void validateRejectsNullRequest() {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(null));

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsWrongProtocolVersion() {
        JsonRpcRequest request = new JsonRpcRequest("1.0", IntNode.valueOf(1), "ping", null, true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsMissingMethod() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), " ", null, true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsInvalidIdType() {
        JsonRpcRequest request = new JsonRpcRequest(
                "2.0",
                OBJECT_MAPPER.createObjectNode().put("x", 1),
                "ping",
                null,
                true
        );

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsPrimitiveParams() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "ping", IntNode.valueOf(3), true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
    }

    @Test
    void validateAllowsTextOrNumberOrNullId() {
        assertDoesNotThrow(() -> validator.validate(
                new JsonRpcRequest("2.0", StringNode.valueOf("abc"), "ping", null, true)));
        assertDoesNotThrow(() -> validator.validate(
                new JsonRpcRequest("2.0", IntNode.valueOf(7), "ping", null, true)));
        assertDoesNotThrow(() -> validator.validate(
                new JsonRpcRequest("2.0", NullNode.getInstance(), "ping", null, true)));
    }

    @Test
    void validateAllowsObjectAndArrayParams() throws Exception {
        assertDoesNotThrow(() -> validator.validate(new JsonRpcRequest(
                "2.0",
                IntNode.valueOf(1),
                "ping",
                OBJECT_MAPPER.readTree("{\"x\":1}"),
                true
        )));

        assertDoesNotThrow(() -> validator.validate(new JsonRpcRequest(
                "2.0",
                IntNode.valueOf(2),
                "ping",
                OBJECT_MAPPER.readTree("[1,2,3]"),
                true
        )));
    }

    @Test
    void validateAllowsNotificationWithoutId() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", null, "ping", null, false);
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
