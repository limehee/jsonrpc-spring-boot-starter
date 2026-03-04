package com.limehee.jsonrpc.sample;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.node.StringNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class GreetingRpcServiceConflictPolicyIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void failsStartupWhenConflictPolicyIsReject() {
        try {
            runContext("REJECT").close();
            fail("Expected startup failure");
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            assertTrue(root.getMessage().contains("method is already registered: ping"));
        }
    }

    @Test
    void usesLastRegistrationWhenConflictPolicyIsReplace() throws Exception {
        try (ConfigurableApplicationContext context = runContext("REPLACE")) {
            JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
            JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """));

            assertEquals("pong", result.singleResponse().orElseThrow().result().asString());
        }
    }

    private ConfigurableApplicationContext runContext(String conflictPolicy) {
        return new SpringApplicationBuilder(ConflictPolicyTestApplication.class)
            .properties(
                "spring.main.web-application-type=none",
                "jsonrpc.method-registration-conflict-policy=" + conflictPolicy
            )
            .run();
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = GreetingRpcService.class)
    static class ConflictPolicyTestApplication {

        @Bean
        JsonRpcMethodRegistration conflictingPingRegistration() {
            return JsonRpcMethodRegistration.of("ping", params -> StringNode.valueOf("manual-ping"));
        }
    }
}
