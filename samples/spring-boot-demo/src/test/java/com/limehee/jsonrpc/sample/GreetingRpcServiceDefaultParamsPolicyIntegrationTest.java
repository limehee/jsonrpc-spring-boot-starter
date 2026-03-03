package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GreetingRpcServiceDefaultParamsPolicyIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void mapsParamsTypeViolationToInvalidParamsByDefault() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"sum","params":"invalid-shape","id":42}
                """);

        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, body.get("error").get("code").asInt());
    }
}
