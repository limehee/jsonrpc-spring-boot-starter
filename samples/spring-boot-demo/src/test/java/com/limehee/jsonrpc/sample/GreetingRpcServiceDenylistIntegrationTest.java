package com.limehee.jsonrpc.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "jsonrpc.method-denylist[0]=ping"
})
class GreetingRpcServiceDenylistIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void denylistBlocksConfiguredMethod() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);

        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, body.get("error").get("code").asInt());
    }

    @Test
    void denylistStillAllowsOtherMethods() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"greet","params":{"name":"codex"},"id":2}
                """);

        assertEquals("hello codex", body.get("result").asText());
    }
}
