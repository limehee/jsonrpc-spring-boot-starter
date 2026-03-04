package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonRpcResponseValidationOptionsTest {

    @Test
    void defaultsEnableRfcMustRules() {
        JsonRpcResponseValidationOptions options = JsonRpcResponseValidationOptions.defaults();

        assertTrue(options.requireJsonRpcVersion20());
        assertTrue(options.requireResponseIdMember());
        assertTrue(options.allowNullResponseId());
        assertTrue(options.allowStringResponseId());
        assertTrue(options.allowNumericResponseId());
        assertTrue(options.allowFractionalResponseId());
        assertTrue(options.requireExclusiveResultOrError());
        assertTrue(options.requireErrorObjectWhenPresent());
        assertTrue(options.requireIntegerErrorCode());
        assertTrue(options.requireStringErrorMessage());
        assertTrue(options.allowRequestFieldsInResponse());
    }

    @Test
    void builderAllowsOverridingEachFlag() {
        JsonRpcResponseValidationOptions options = JsonRpcResponseValidationOptions.builder()
            .requireJsonRpcVersion20(false)
            .requireResponseIdMember(false)
            .allowNullResponseId(false)
            .allowStringResponseId(false)
            .allowNumericResponseId(false)
            .allowFractionalResponseId(false)
            .requireExclusiveResultOrError(false)
            .requireErrorObjectWhenPresent(false)
            .requireIntegerErrorCode(false)
            .requireStringErrorMessage(false)
            .allowRequestFieldsInResponse(false)
            .build();

        assertFalse(options.requireJsonRpcVersion20());
        assertFalse(options.requireResponseIdMember());
        assertFalse(options.allowNullResponseId());
        assertFalse(options.allowStringResponseId());
        assertFalse(options.allowNumericResponseId());
        assertFalse(options.allowFractionalResponseId());
        assertFalse(options.requireExclusiveResultOrError());
        assertFalse(options.requireErrorObjectWhenPresent());
        assertFalse(options.requireIntegerErrorCode());
        assertFalse(options.requireStringErrorMessage());
        assertFalse(options.allowRequestFieldsInResponse());
    }
}
