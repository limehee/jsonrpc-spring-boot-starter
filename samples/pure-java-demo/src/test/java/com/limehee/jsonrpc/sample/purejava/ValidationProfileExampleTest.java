package com.limehee.jsonrpc.sample.purejava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationProfileExampleTest {

    @Test
    void strictRequestProfileRejectsNotificationWithoutId() throws Exception {
        JsonRpcDispatchResult result = ValidationProfileExample.dispatchStrict("""
            {"jsonrpc":"2.0","method":"ping"}
            """);

        assertTrue(result.hasResponse());
        assertEquals(-32600, result.singleResponse().orElseThrow().error().code());
        assertNull(result.singleResponse().orElseThrow().id());
    }

    @Test
    void strictRequestProfileRejectsFractionalId() throws Exception {
        JsonRpcDispatchResult result = ValidationProfileExample.dispatchStrict("""
            {"jsonrpc":"2.0","method":"ping","id":1.5}
            """);

        assertEquals(-32600, result.singleResponse().orElseThrow().error().code());
    }

    @Test
    void strictRequestProfileRejectsResponseFieldsInsideRequest() throws Exception {
        JsonRpcDispatchResult result = ValidationProfileExample.dispatchStrict("""
            {"jsonrpc":"2.0","method":"ping","id":1,"result":"unexpected"}
            """);

        assertEquals(-32600, result.singleResponse().orElseThrow().error().code());
    }

    @Test
    void strictResponseProfileAcceptsStandardServerErrorRange() {
        List<JsonRpcIncomingResponse> responses = ValidationProfileExample.parseAndValidateStrictResponses("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"server"}}
            """);

        assertEquals(1, responses.size());
        assertEquals(-32000, responses.get(0).error().get("code").asInt());
    }

    @Test
    void strictResponseProfileRejectsDuplicateMembers() {
        assertThrows(JsonRpcException.class, () -> ValidationProfileExample.parseAndValidateStrictResponses("""
            {"jsonrpc":"2.0","id":1,"id":2,"result":"pong"}
            """));
    }

    @Test
    void strictResponseProfileRejectsRequestFields() {
        assertThrows(JsonRpcException.class, () -> ValidationProfileExample.parseAndValidateStrictResponses("""
            {"jsonrpc":"2.0","id":1,"result":"pong","method":"ping"}
            """));
    }
}
