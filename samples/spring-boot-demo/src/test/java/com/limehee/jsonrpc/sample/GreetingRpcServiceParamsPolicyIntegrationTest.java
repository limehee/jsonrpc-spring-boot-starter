package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.JsonRpcParamsTypeViolationCodePolicy;
import com.limehee.jsonrpc.core.JsonRpcRequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(GreetingRpcServiceParamsPolicyIntegrationTest.StrictParamsPolicyConfig.class)
class GreetingRpcServiceParamsPolicyIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void mapsParamsTypeViolationToInvalidRequestWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"sum","params":"invalid-shape","id":41}
                """);

        assertEquals(-32600, body.get("error").get("code").asInt());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StrictParamsPolicyConfig {
        @Bean
        JsonRpcRequestValidator jsonRpcRequestValidator() {
            return new DefaultJsonRpcRequestValidator(JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST);
        }
    }
}
