package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "jsonrpc.method-allowlist[0]=ping",
        "jsonrpc.method-denylist[0]=ping"
})
class GreetingRpcServiceAccessPrecedenceIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void denylistTakesPrecedenceOverAllowlist() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);

        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, body.get("error").get("code").asInt());
    }
}
