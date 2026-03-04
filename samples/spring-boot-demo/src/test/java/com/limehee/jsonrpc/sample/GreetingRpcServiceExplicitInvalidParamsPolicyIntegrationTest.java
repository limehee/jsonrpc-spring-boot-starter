package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "jsonrpc.validation.request.params-type-violation-code-policy=INVALID_PARAMS")
class GreetingRpcServiceExplicitInvalidParamsPolicyIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void mapsParamsTypeViolationToInvalidParamsWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"sum","params":"invalid-shape","id":43}
            """);

        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, body.get("error").get("code").asInt());
    }
}
