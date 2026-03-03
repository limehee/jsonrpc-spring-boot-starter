package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultJsonRpcResponseValidatorTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final JsonRpcResponseParser PARSER = new DefaultJsonRpcResponseParser();

    private final JsonRpcResponseValidator validator = new DefaultJsonRpcResponseValidator();

    @Test
    void validateAcceptsValidResultResponse() throws Exception {
        assertDoesNotThrow(() -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"result":{"ok":true}}
                """)));
    }

    @Test
    void validateAcceptsValidErrorResponse() throws Exception {
        assertDoesNotThrow(() -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":"abc","error":{"code":-32000,"message":"x"}}
                """)));
    }

    @Test
    void validateRejectsNullResponse() {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(null));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsWrongProtocolVersionByDefault() throws Exception {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"1.0","id":1,"result":1}
                """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateAllowsWrongProtocolVersionWhenDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder()
                        .requireJsonRpcVersion20(false)
                        .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
                {"jsonrpc":"1.0","id":1,"result":1}
                """)));
    }

    @Test
    void validateRejectsMissingIdByDefault() throws Exception {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","result":1}
                """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateAllowsMissingIdWhenRuleDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder()
                        .requireResponseIdMember(false)
                        .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
                {"jsonrpc":"2.0","result":1}
                """)));
    }

    @Test
    void validateRejectsInvalidIdTypes() throws Exception {
        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":{"x":1},"result":1}
                """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRespectsIdTypeOptions() throws Exception {
        JsonRpcResponseValidator noNull = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder().allowNullResponseId(false).build()
        );
        assertThrows(JsonRpcException.class, () -> noNull.validate(incoming("""
                {"jsonrpc":"2.0","id":null,"result":1}
                """)));

        JsonRpcResponseValidator noString = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder().allowStringResponseId(false).build()
        );
        assertThrows(JsonRpcException.class, () -> noString.validate(incoming("""
                {"jsonrpc":"2.0","id":"x","result":1}
                """)));

        JsonRpcResponseValidator noNumeric = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder().allowNumericResponseId(false).build()
        );
        assertThrows(JsonRpcException.class, () -> noNumeric.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"result":1}
                """)));
    }

    @Test
    void validateRejectsFractionalIdWhenDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder()
                        .allowFractionalResponseId(false)
                        .build()
        );

        assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
                {"jsonrpc":"2.0","id":1.5,"result":1}
                """)));
    }

    @Test
    void validateRejectsInvalidResultAndErrorCombination() throws Exception {
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"result":1,"error":{"code":-32000,"message":"x"}}
                """)));
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1}
                """)));
    }

    @Test
    void validateRejectsInvalidErrorObjectStructure() throws Exception {
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"error":1}
                """)));
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"error":{"code":-32000}}
                """)));
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"error":{"code":1.5,"message":"x"}}
                """)));
    }

    @Test
    void validateCanRejectRequestFieldsWhenOptionDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
                JsonRpcResponseValidationOptions.builder()
                        .allowRequestFieldsInResponse(false)
                        .build()
        );

        assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"result":1,"method":"ping"}
                """)));
    }

    @Test
    void validateAllowsRequestFieldsByDefault() throws Exception {
        assertDoesNotThrow(() -> validator.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"result":1,"method":"ping","params":{"a":1}}
                """)));
    }

    private JsonRpcIncomingResponse incoming(String json) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree(json);
        return PARSER.parse(payload).singleResponse().orElseThrow();
    }
}
