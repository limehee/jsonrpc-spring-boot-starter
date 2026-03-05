package com.limehee.jsonrpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.limehee.jsonrpc.core.DefaultJsonRpcResponseParser;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponse;
import com.limehee.jsonrpc.core.JsonRpcResponseValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;

@SpringBootTest(properties = {
    "jsonrpc.validation.request.require-id-member=true",
    "jsonrpc.validation.request.allow-fractional-id=false",
    "jsonrpc.validation.request.reject-response-fields=true",
    "jsonrpc.validation.request.reject-duplicate-members=true",
    "jsonrpc.validation.response.reject-duplicate-members=true",
    "jsonrpc.validation.response.reject-request-fields=true",
    "jsonrpc.validation.response.error-code.policy=STANDARD_ONLY"
})
class GreetingRpcServiceValidationProfilesIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Autowired
    private DefaultJsonRpcResponseParser responseParser;

    @Autowired
    private JsonRpcResponseValidator responseValidator;

    @Test
    void rejectsNotificationWithoutIdWhenRequireIdMemberEnabled() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"ping"}
            """);

        assertEquals(-32600, body.get("error").get("code").asInt());
    }

    @Test
    void rejectsResponseFieldsInsideRequestWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"ping","id":1,"result":"unexpected"}
            """);

        assertEquals(-32600, body.get("error").get("code").asInt());
    }

    @Test
    void rejectsFractionalRequestIdWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"ping","id":1.5}
            """);

        assertEquals(-32600, body.get("error").get("code").asInt());
    }

    @Test
    void rejectsDuplicateRequestMembersWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"ping","id":1,"id":2}
            """);

        assertEquals(-32700, body.get("error").get("code").asInt());
    }

    @Test
    void responseParserRejectsDuplicateMembersWhenConfigured() {
        assertThrows(JsonRpcException.class, () -> responseParser.parse("""
            {"jsonrpc":"2.0","id":1,"id":2,"result":"pong"}
            """));
    }

    @Test
    void responseValidatorAppliesStandardOnlyErrorCodePolicy() {
        JsonRpcIncomingResponse standardError = responseParser.parse("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32603,"message":"internal"}}
            """).singleResponse().orElseThrow();
        JsonRpcIncomingResponse serverRangeError = responseParser.parse("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"server"}}
            """).singleResponse().orElseThrow();

        responseValidator.validate(standardError);
        assertThrows(JsonRpcException.class, () -> responseValidator.validate(serverRangeError));
    }

    @Test
    void responseValidatorRejectsRequestFieldsWhenConfigured() {
        JsonRpcIncomingResponse pollutedResponse = responseParser.parse("""
            {"jsonrpc":"2.0","id":1,"result":"ok","method":"ping"}
            """).singleResponse().orElseThrow();

        assertThrows(JsonRpcException.class, () -> responseValidator.validate(pollutedResponse));
    }
}
