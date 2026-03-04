package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(GreetingRpcServiceCustomExceptionResolverIntegrationTest.CustomResolverConfig.class)
class GreetingRpcServiceCustomExceptionResolverIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void mapsDomainExceptionWithCustomResolver() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"domain.fail","id":91}
            """);

        assertEquals(-32051, body.get("error").get("code").asInt());
        assertEquals("custom-domain-error", body.get("error").get("message").asString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomResolverConfig {

        @Bean
        JsonRpcExceptionResolver jsonRpcExceptionResolver() {
            return throwable -> {
                if (throwable instanceof IllegalArgumentException) {
                    return JsonRpcError.of(-32051, "custom-domain-error");
                }
                return JsonRpcError.of(-32603, "fallback");
            };
        }

        @Bean
        JsonRpcMethodRegistration domainFailMethod() {
            return JsonRpcMethodRegistration.of("domain.fail", params -> {
                throw new IllegalArgumentException("invalid-domain-state");
            });
        }
    }
}
