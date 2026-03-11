package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
        assertEquals(JsonRpcConstants.MESSAGE_INVALID_REQUEST, ex.getMessage());
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
                .requireIdMember(false)
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
            JsonRpcResponseValidationOptions.builder().allowNullId(false).build()
        );
        assertThrows(JsonRpcException.class, () -> noNull.validate(incoming("""
            {"jsonrpc":"2.0","id":null,"result":1}
            """)));

        JsonRpcResponseValidator noString = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder().allowStringId(false).build()
        );
        assertThrows(JsonRpcException.class, () -> noString.validate(incoming("""
            {"jsonrpc":"2.0","id":"x","result":1}
            """)));

        JsonRpcResponseValidator noNumeric = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder().allowNumericId(false).build()
        );
        assertThrows(JsonRpcException.class, () -> noNumeric.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"result":1}
            """)));
    }

    @Test
    void validateRejectsFractionalIdWhenDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .allowFractionalId(false)
                .build()
        );

        assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1.5,"result":1}
            """)));
    }

    @Test
    void validateAllowsNullIdWhenOptionIsExplicitlyEnabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .allowNullId(true)
                .allowStringId(false)
                .allowNumericId(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":null,"result":1}
            """)));
    }

    @Test
    void validateAllowsStringIdWhenOptionIsExplicitlyEnabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .allowNullId(false)
                .allowStringId(true)
                .allowNumericId(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":"abc","result":1}
            """)));
    }

    @Test
    void validateAllowsNumericIdWhenOptionIsExplicitlyEnabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .allowNullId(false)
                .allowStringId(false)
                .allowNumericId(true)
                .allowFractionalId(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"result":1}
            """)));
    }

    @Test
    void validateAllowsFractionalIdWhenOptionIsExplicitlyEnabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .allowNullId(false)
                .allowStringId(false)
                .allowNumericId(true)
                .allowFractionalId(true)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
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
    void validateAllowsResultAndErrorCombinationWhenExclusiveRuleDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .requireExclusiveResultOrError(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"result":1,"error":{"code":-32000,"message":"x"}}
            """)));
    }

    @Test
    void validateAllowsMissingResultAndErrorWhenExclusiveRuleDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .requireExclusiveResultOrError(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
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
    void validateRejectsNonNumericErrorCodeAndNonStringErrorMessage() throws Exception {
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":"x","message":"err"}}
            """)));
        assertThrows(JsonRpcException.class, () -> validator.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":3}}
            """)));
    }

    @Test
    void validateAllowsNonObjectErrorWhenObjectRuleIsDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .requireErrorObjectWhenPresent(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":1}
            """)));
    }

    @Test
    void validateAllowsMissingErrorCodeAndMessageWhenRulesAreDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .requireIntegerErrorCode(false)
                .requireStringErrorMessage(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"data":{"x":1}}}
            """)));
    }

    @Test
    void validateAllowsNonIntegerErrorCodeWhenIntegerRuleIsDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .requireIntegerErrorCode(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":"x","message":"err"}}
            """)));
    }

    @Test
    void validateAllowsNonStringErrorMessageWhenStringRuleIsDisabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .requireStringErrorMessage(false)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":3}}
            """)));
    }

    @Test
    void validateRejectsRequestFieldsWhenOptionEnabled() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .rejectRequestFields(true)
                .build()
        );

        assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"result":1,"method":"ping"}
            """)));
    }

    @Test
    void validateSkipsRequestFieldRejectionWhenSourceIsUnavailable() {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .rejectRequestFields(true)
                .build()
        );
        JsonRpcIncomingResponse response = new JsonRpcIncomingResponse(
            "2.0",
            OBJECT_MAPPER.getNodeFactory().numberNode(1),
            true,
            OBJECT_MAPPER.getNodeFactory().numberNode(1),
            true,
            null,
            false
        );

        assertDoesNotThrow(() -> custom.validate(response));
    }

    @Test
    void validateAllowsRequestFieldsByDefault() throws Exception {
        assertDoesNotThrow(() -> validator.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"result":1,"method":"ping","params":{"a":1}}
            """)));
    }

    @Test
    void validateAllowsFractionalIdByDefault() throws Exception {
        assertDoesNotThrow(() -> validator.validate(incoming("""
            {"jsonrpc":"2.0","id":1.5,"result":1}
            """)));
    }

    @Test
    void constructorRejectsNullOptions() {
        assertThrows(NullPointerException.class, () -> new DefaultJsonRpcResponseValidator(null));
    }

    @Test
    void validateRejectsNonStandardErrorCodeWhenPolicyIsStandardOnly() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_ONLY)
                .build()
        );

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
                {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"server"}}
            """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateRejectsCustomErrorCodeWhenPolicyIsStandardOnly() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_ONLY)
                .build()
        );

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":1001,"message":"custom"}}
            """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateAllowsStandardErrorCodeWhenPolicyIsStandardOnly() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_ONLY)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"missing"}}
            """)));
    }

    @Test
    void validateAllowsServerErrorRangeWhenPolicyConfigured() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_OR_SERVER_ERROR_RANGE)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32050,"message":"server"}}
            """)));
        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"missing"}}
            """)));
    }

    @Test
    void validateAllowsServerErrorRangeBoundariesWhenPolicyConfigured() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_OR_SERVER_ERROR_RANGE)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32099,"message":"server"}}
            """)));
        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"server"}}
            """)));
    }

    @Test
    void validateRejectsOutsideServerErrorRangeWhenPolicyConfigured() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_OR_SERVER_ERROR_RANGE)
                .build()
        );

        assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32100,"message":"server"}}
            """)));
    }

    @Test
    void validateRejectsOutOfRangeCustomErrorCode() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
                .errorCodeRangeMin(-45000)
                .errorCodeRangeMax(-44000)
                .build()
        );

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32050,"message":"server"}}
            """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    @Test
    void validateAllowsInRangeCustomErrorCode() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
                .errorCodeRangeMin(-45000)
                .errorCodeRangeMax(-44000)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-44500,"message":"custom"}}
            """)));
    }

    @Test
    void validateAllowsStandardCodeWhenCustomRangeContainsIt() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
                .errorCodeRangeMin(-40000)
                .errorCodeRangeMax(-30000)
                .build()
        );

        assertDoesNotThrow(() -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"missing"}}
            """)));
    }

    @Test
    void validateRejectsStandardCodeWhenCustomRangeDoesNotContainIt() throws Exception {
        JsonRpcResponseValidator custom = new DefaultJsonRpcResponseValidator(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
                .errorCodeRangeMin(1000)
                .errorCodeRangeMax(2000)
                .build()
        );

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> custom.validate(incoming("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"missing"}}
            """)));
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode());
    }

    private JsonRpcIncomingResponse incoming(String json) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree(json);
        return PARSER.parse(payload).singleResponse().orElseThrow();
    }
}
