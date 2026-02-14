package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonRpcAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JsonRpcAutoConfiguration.class));

    @Test
    void createsDispatcherAndRegistersMethods() {
        contextRunner
                .withBean("ping", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    assertNotNull(dispatcher);

                    JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "ping", null, true);
                    JsonRpcResponse response = dispatcher.dispatch(request);
                    assertNotNull(response);
                    assertEquals("pong", response.result().asText());

                    JsonRpcTypedMethodHandlerFactory typedFactory = context.getBean(JsonRpcTypedMethodHandlerFactory.class);
                    assertNotNull(typedFactory);
                });
    }

    @Test
    void registersAnnotatedMethods() throws Exception {
        contextRunner
                .withUserConfiguration(AnnotatedMethodConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    assertNotNull(dispatcher);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(7),
                            "hello",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"name\":\"codex\"}"),
                            true
                    ));

                    assertNotNull(response);
                    assertEquals("hello codex", response.result().asText());
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedMethodConfig {
        @Bean
        AnnotatedHandler annotatedHandler() {
            return new AnnotatedHandler();
        }
    }

    static class AnnotatedHandler {
        @JsonRpcMethod("hello")
        public String hello(NameParams params) {
            return "hello " + params.name();
        }
    }

    record NameParams(String name) {
    }
}
