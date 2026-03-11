package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultJsonRpcErrorClassifierTest {

    private final JsonRpcErrorClassifier classifier = new DefaultJsonRpcErrorClassifier();

    @Test
    void classifyReturnsStandardForAllStandardJsonRpcErrorCodes() {
        assertSame(JsonRpcErrorCodeCategory.STANDARD, classifier.classify(JsonRpcErrorCode.PARSE_ERROR));
        assertSame(JsonRpcErrorCodeCategory.STANDARD, classifier.classify(JsonRpcErrorCode.INVALID_REQUEST));
        assertSame(JsonRpcErrorCodeCategory.STANDARD, classifier.classify(JsonRpcErrorCode.METHOD_NOT_FOUND));
        assertSame(JsonRpcErrorCodeCategory.STANDARD, classifier.classify(JsonRpcErrorCode.INVALID_PARAMS));
        assertSame(JsonRpcErrorCodeCategory.STANDARD, classifier.classify(JsonRpcErrorCode.INTERNAL_ERROR));
    }

    @Test
    void classifyReturnsServerReservedRangeForBoundaryValues() {
        assertSame(JsonRpcErrorCodeCategory.SERVER_RESERVED_RANGE, classifier.classify(-32099));
        assertSame(JsonRpcErrorCodeCategory.SERVER_RESERVED_RANGE, classifier.classify(-32000));
    }

    @Test
    void classifyReturnsCustomForValuesOutsideServerReservedRange() {
        assertSame(JsonRpcErrorCodeCategory.CUSTOM, classifier.classify(-32100));
        assertSame(JsonRpcErrorCodeCategory.CUSTOM, classifier.classify(-31999));
        assertSame(JsonRpcErrorCodeCategory.CUSTOM, classifier.classify(-40000));
        assertSame(JsonRpcErrorCodeCategory.CUSTOM, classifier.classify(0));
        assertSame(JsonRpcErrorCodeCategory.CUSTOM, classifier.classify(1));
        assertSame(JsonRpcErrorCodeCategory.CUSTOM, classifier.classify(1001));
    }

    @Test
    void helperMethodsReflectClassificationResult() {
        assertTrue(classifier.isStandard(JsonRpcErrorCode.METHOD_NOT_FOUND));
        assertFalse(classifier.isServerErrorRange(JsonRpcErrorCode.METHOD_NOT_FOUND));
        assertFalse(classifier.isCustom(JsonRpcErrorCode.METHOD_NOT_FOUND));

        assertTrue(classifier.isServerErrorRange(-32050));
        assertFalse(classifier.isStandard(-32050));
        assertFalse(classifier.isCustom(-32050));

        assertTrue(classifier.isCustom(1001));
        assertFalse(classifier.isStandard(1001));
        assertFalse(classifier.isServerErrorRange(1001));
    }
}
