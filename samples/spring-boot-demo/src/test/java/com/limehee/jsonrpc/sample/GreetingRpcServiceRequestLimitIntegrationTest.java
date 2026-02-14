package com.limehee.jsonrpc.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "jsonrpc.max-request-bytes=8"
})
class GreetingRpcServiceRequestLimitIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void returnsInvalidRequestWhenPayloadIsTooLarge() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, body.get("error").get("code").asInt());
        assertTrue(body.get("id").isNull());
    }
}
