package com.limehee.jsonrpc.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "jsonrpc.method-allowlist[0]=ping"
})
class GreetingRpcServiceAllowlistIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void allowlistPermitsConfiguredMethod() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);

        assertEquals("pong", body.get("result").asText());
    }

    @Test
    void allowlistBlocksMethodsOutsideAllowlist() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"greet","params":{"name":"codex"},"id":2}
                """);

        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, body.get("error").get("code").asInt());
    }
}
