package com.limehee.jsonrpc.sample.purejava;

import com.limehee.jsonrpc.core.JsonRpcEnvelopeType;
import com.limehee.jsonrpc.core.JsonRpcException;
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
}
