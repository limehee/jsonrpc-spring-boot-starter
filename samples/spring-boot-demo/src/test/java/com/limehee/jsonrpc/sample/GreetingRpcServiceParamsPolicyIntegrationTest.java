package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "jsonrpc.validation.request.params-type-violation-code-policy=INVALID_REQUEST")
class GreetingRpcServiceParamsPolicyIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void mapsParamsTypeViolationToInvalidRequestWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"sum","params":"invalid-shape","id":41}
            """);

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, body.get("error").get("code").asInt());
    }
}
