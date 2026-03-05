package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

class DefaultJsonRpcRequestValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final JsonRpcRequestParser REQUEST_PARSER = new DefaultJsonRpcRequestParser();

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
    void validateAllowsWrongProtocolVersionWhenDisabled() {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .requireJsonRpcVersion20(false)
                .build()
        );
        JsonRpcRequest request = new JsonRpcRequest("1.0", IntNode.valueOf(1), "ping", null, true);

        assertDoesNotThrow(() -> custom.validate(request));
    }

    @Test
    void validateRejectsMissingMethod() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), " ", null, true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsReservedMethodNamespace() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "rpc.system", null, true);

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
    void validateRejectsMissingIdWhenConfigured() {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .requireIdMember(true)
                .build()
        );

        JsonRpcRequest request = new JsonRpcRequest("2.0", null, "ping", null, false);
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsNullIdWhenDisabled() {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .allowNullId(false)
                .build()
        );
        JsonRpcRequest request = new JsonRpcRequest("2.0", NullNode.getInstance(), "ping", null, true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsStringIdWhenDisabled() {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .allowStringId(false)
                .build()
        );
        JsonRpcRequest request = new JsonRpcRequest("2.0", StringNode.valueOf("abc"), "ping", null, true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsNumericIdWhenDisabled() {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .allowNumericId(false)
                .build()
        );
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(7), "ping", null, true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsFractionalIdWhenDisabled() throws Exception {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .allowFractionalId(false)
                .build()
        );
        JsonRpcRequest request = REQUEST_PARSER.parse(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":1.5}
            """));

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsPrimitiveParams() {
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "ping", IntNode.valueOf(3), true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, ex.getCode());
    }

    @Test
    void validateRejectsPrimitiveParamsAsInvalidRequestWhenPolicyIsConfigured() {
        DefaultJsonRpcRequestValidator strictShapeValidator = new DefaultJsonRpcRequestValidator(
            JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST
        );
        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "ping", IntNode.valueOf(3), true);

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> strictShapeValidator.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void constructorRejectsNullParamsTypeViolationPolicy() {
        assertThrows(
            NullPointerException.class,
            () -> new DefaultJsonRpcRequestValidator((JsonRpcParamsTypeViolationCodePolicy) null)
        );
    }

    @Test
    void constructorRejectsNullOptions() {
        assertThrows(
            NullPointerException.class,
            () -> new DefaultJsonRpcRequestValidator((JsonRpcRequestValidationOptions) null)
        );
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

    @Test
    void validateRejectsRequestContainingResponseFieldsWhenConfigured() throws Exception {
        DefaultJsonRpcRequestValidator custom = new DefaultJsonRpcRequestValidator(
            JsonRpcRequestValidationOptions.builder()
                .rejectResponseFields(true)
                .build()
        );
        JsonRpcRequest request = REQUEST_PARSER.parse(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":1,"result":1}
            """));

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(request));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateAllowsRequestContainingResponseFieldsByDefault() throws Exception {
        JsonRpcRequest request = REQUEST_PARSER.parse(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":1,"result":1}
            """));
        assertDoesNotThrow(() -> validator.validate(request));
    }
}
