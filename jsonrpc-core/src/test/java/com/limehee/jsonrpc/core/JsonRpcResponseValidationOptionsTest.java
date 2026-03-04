package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JsonRpcResponseValidationOptionsTest {

    @Test
    void defaultsEnableRfcMustRules() {
        JsonRpcResponseValidationOptions options = JsonRpcResponseValidationOptions.defaults();

        assertTrue(options.requireJsonRpcVersion20());
        assertTrue(options.requireIdMember());
        assertTrue(options.allowNullId());
        assertTrue(options.allowStringId());
        assertTrue(options.allowNumericId());
        assertTrue(options.allowFractionalId());
        assertTrue(options.requireExclusiveResultOrError());
        assertTrue(options.requireErrorObjectWhenPresent());
        assertTrue(options.requireIntegerErrorCode());
        assertTrue(options.requireStringErrorMessage());
        assertFalse(options.rejectRequestFields());
        assertFalse(options.rejectDuplicateMembers());
        assertTrue(options.errorCodePolicy() == JsonRpcResponseErrorCodePolicy.ANY_INTEGER);
    }

    @Test
    void builderAllowsOverridingEachFlag() {
        JsonRpcResponseValidationOptions options = JsonRpcResponseValidationOptions.builder()
            .requireJsonRpcVersion20(false)
            .requireIdMember(false)
            .allowNullId(false)
            .allowStringId(false)
            .allowNumericId(false)
            .allowFractionalId(false)
            .requireExclusiveResultOrError(false)
            .requireErrorObjectWhenPresent(false)
            .requireIntegerErrorCode(false)
            .requireStringErrorMessage(false)
            .rejectRequestFields(true)
            .rejectDuplicateMembers(true)
            .build();

        assertFalse(options.requireJsonRpcVersion20());
        assertFalse(options.requireIdMember());
        assertFalse(options.allowNullId());
        assertFalse(options.allowStringId());
        assertFalse(options.allowNumericId());
        assertFalse(options.allowFractionalId());
        assertFalse(options.requireExclusiveResultOrError());
        assertFalse(options.requireErrorObjectWhenPresent());
        assertFalse(options.requireIntegerErrorCode());
        assertFalse(options.requireStringErrorMessage());
        assertTrue(options.rejectRequestFields());
        assertTrue(options.rejectDuplicateMembers());
    }

    @Test
    void builderRejectsIncompatibleErrorCodePolicyWhenIntegerCheckDisabled() {
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonRpcResponseValidationOptions.builder()
                .requireIntegerErrorCode(false)
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_ONLY)
                .build()
        );
    }

    @Test
    void builderRejectsIncompleteCustomRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
                .errorCodeRangeMin(-32603)
                .build()
        );
    }

    @Test
    void builderRejectsInvertedCustomRange() {
        assertThrows(
            IllegalArgumentException.class,
            () -> JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
                .errorCodeRangeMin(-32000)
                .errorCodeRangeMax(-32099)
                .build()
        );
    }

    @Test
    void builderAcceptsCustomRangePolicy() {
        JsonRpcResponseValidationOptions options = JsonRpcResponseValidationOptions.builder()
            .errorCodePolicy(JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE)
            .errorCodeRangeMin(-45000)
            .errorCodeRangeMax(-44000)
            .build();

        assertTrue(options.errorCodePolicy() == JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE);
        assertTrue(options.errorCodeRangeMin() == -45000);
        assertTrue(options.errorCodeRangeMax() == -44000);
    }
}
