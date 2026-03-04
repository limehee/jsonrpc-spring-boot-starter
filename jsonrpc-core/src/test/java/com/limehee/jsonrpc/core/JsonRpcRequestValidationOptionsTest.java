package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonRpcRequestValidationOptionsTest {

    @Test
    void defaultsAlignWithJsonRpcRequestSemantics() {
        JsonRpcRequestValidationOptions options = JsonRpcRequestValidationOptions.defaults();

        assertTrue(options.requireJsonRpcVersion20());
        assertFalse(options.requireIdMember());
        assertTrue(options.allowNullId());
        assertTrue(options.allowStringId());
        assertTrue(options.allowNumericId());
        assertTrue(options.allowFractionalId());
        assertFalse(options.rejectResponseFields());
        assertFalse(options.rejectDuplicateMembers());
        assertTrue(options.paramsTypeViolationCodePolicy() == JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);
    }

    @Test
    void builderAllowsExplicitOverrides() {
        JsonRpcRequestValidationOptions options = JsonRpcRequestValidationOptions.builder()
            .requireJsonRpcVersion20(false)
            .requireIdMember(true)
            .allowNullId(false)
            .allowStringId(false)
            .allowNumericId(false)
            .allowFractionalId(false)
            .rejectResponseFields(true)
            .rejectDuplicateMembers(true)
            .paramsTypeViolationCodePolicy(JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST)
            .build();

        assertFalse(options.requireJsonRpcVersion20());
        assertTrue(options.requireIdMember());
        assertFalse(options.allowNullId());
        assertFalse(options.allowStringId());
        assertFalse(options.allowNumericId());
        assertFalse(options.allowFractionalId());
        assertTrue(options.rejectResponseFields());
        assertTrue(options.rejectDuplicateMembers());
        assertTrue(options.paramsTypeViolationCodePolicy() == JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST);
    }

    @Test
    void builderRejectsNullParamsTypeViolationPolicy() {
        assertThrows(
            NullPointerException.class,
            () -> JsonRpcRequestValidationOptions.builder().paramsTypeViolationCodePolicy(null)
        );
    }
}
