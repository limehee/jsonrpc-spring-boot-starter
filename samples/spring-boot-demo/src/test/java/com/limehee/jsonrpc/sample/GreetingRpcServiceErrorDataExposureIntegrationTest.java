package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "jsonrpc.include-error-data=true")
@Import(GreetingRpcServiceErrorDataExposureIntegrationTest.BoomMethodConfig.class)
class GreetingRpcServiceErrorDataExposureIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void includesErrorDataWhenConfigured() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"boom.with-data","id":81}
                """);

        assertEquals(-32011, body.get("error").get("code").asInt());
        assertEquals("sensitive-context", body.get("error").get("data").asString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BoomMethodConfig {
        @Bean
        JsonRpcMethodRegistration boomWithDataMethod() {
            return JsonRpcMethodRegistration.of("boom.with-data", params -> {
                throw new JsonRpcException(-32011, "domain-with-data", StringNode.valueOf("sensitive-context"));
            });
        }
    }
}
