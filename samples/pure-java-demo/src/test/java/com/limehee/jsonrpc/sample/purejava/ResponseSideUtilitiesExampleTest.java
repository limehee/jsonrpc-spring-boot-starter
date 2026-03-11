package com.limehee.jsonrpc.sample.purejava;

import com.limehee.jsonrpc.core.JsonRpcEnvelopeType;
import com.limehee.jsonrpc.core.JsonRpcErrorCodeCategory;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcResponseErrorCodePolicy;
import com.limehee.jsonrpc.core.JsonRpcResponseValidationOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseSideUtilitiesExampleTest {

    @Test
    void classifiesAndValidatesSingleResponse() throws Exception {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.defaults()
        );

        ResponseSideUtilitiesExample.Result result = example.inspect("""
            {"jsonrpc":"2.0","id":1,"result":"pong"}
            """);

        assertEquals(JsonRpcEnvelopeType.RESPONSE, result.envelopeType());
        assertEquals(1, result.responses().size());
        assertEquals("pong", result.responses().get(0).result().asString());
    }

    @Test
    void classifiesRequestWithoutParsingAsResponse() throws Exception {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.defaults()
        );

        ResponseSideUtilitiesExample.Result result = example.inspect("""
            {"jsonrpc":"2.0","method":"ping","id":1}
            """);

        assertEquals(JsonRpcEnvelopeType.REQUEST, result.envelopeType());
        assertTrue(result.responses().isEmpty());
    }

    @Test
    void validatesBatchResponses() throws Exception {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.defaults()
        );

        ResponseSideUtilitiesExample.Result result = example.inspect("""
            [
              {"jsonrpc":"2.0","id":1,"result":"one"},
              {"jsonrpc":"2.0","id":2,"error":{"code":-32601,"message":"Method not found"}}
            ]
            """);

        assertEquals(JsonRpcEnvelopeType.RESPONSE, result.envelopeType());
        assertEquals(2, result.responses().size());
        assertEquals("one", result.responses().get(0).result().asString());
        assertEquals(-32601, result.responses().get(1).error().get("code").asInt());
    }

    @Test
    void failsValidationForMalformedErrorObject() {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.defaults()
        );

        assertThrows(JsonRpcException.class, () -> example.inspect("""
            {"jsonrpc":"2.0","id":1,"error":{"code":"bad","message":1}}
            """));
    }

    @Test
    void rejectsDuplicateMembersWhenConfigured() {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.builder()
                .rejectDuplicateMembers(true)
                .build()
        );

        assertThrows(JsonRpcException.class, () -> example.inspect("""
            {"jsonrpc":"2.0","id":1,"id":2,"result":"pong"}
            """));
    }

    @Test
    void rejectsRequestFieldsInsideResponseWhenConfigured() {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.builder()
                .rejectRequestFields(true)
                .build()
        );

        assertThrows(JsonRpcException.class, () -> example.inspect("""
            {"jsonrpc":"2.0","id":1,"result":"pong","method":"ping"}
            """));
    }

    @Test
    void classifiesStandardServerRangeAndCustomErrorCodes() {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.defaults()
        );

        assertEquals(JsonRpcErrorCodeCategory.STANDARD, example.classifyErrorCode(-32601));
        assertEquals(JsonRpcErrorCodeCategory.SERVER_RESERVED_RANGE, example.classifyErrorCode(-32001));
        assertEquals(JsonRpcErrorCodeCategory.CUSTOM, example.classifyErrorCode(1001));
    }

    @Test
    void classifiesValidatedErrorCodesFromResponsePayload() throws Exception {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.builder()
                .errorCodePolicy(JsonRpcResponseErrorCodePolicy.ANY_INTEGER)
                .build()
        );

        var classified = example.classifyValidatedErrorCodes("""
            [
              {"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"Method not found"}},
              {"jsonrpc":"2.0","id":2,"error":{"code":-32001,"message":"Server issue"}},
              {"jsonrpc":"2.0","id":3,"error":{"code":1001,"message":"Domain error"}},
              {"jsonrpc":"2.0","id":4,"result":"ok"}
            ]
            """);

        assertEquals(3, classified.size());
        assertEquals(JsonRpcErrorCodeCategory.STANDARD, classified.get(0).category());
        assertEquals(JsonRpcErrorCodeCategory.SERVER_RESERVED_RANGE, classified.get(1).category());
        assertEquals(JsonRpcErrorCodeCategory.CUSTOM, classified.get(2).category());
    }

    @Test
    void skipsErrorCodeClassificationWhenPayloadIsNotResponseEnvelope() throws Exception {
        ResponseSideUtilitiesExample example = new ResponseSideUtilitiesExample(
            JsonRpcResponseValidationOptions.defaults()
        );

        assertTrue(example.classifyValidatedErrorCodes("""
            {"jsonrpc":"2.0","method":"ping","id":1}
            """).isEmpty());
    }
}
