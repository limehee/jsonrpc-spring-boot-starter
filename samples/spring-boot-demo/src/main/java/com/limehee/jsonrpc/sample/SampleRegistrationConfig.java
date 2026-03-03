package com.limehee.jsonrpc.sample;

import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import tools.jackson.databind.node.StringNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class SampleRegistrationConfig {

    @Bean
    JsonRpcMethodRegistration manualEchoRegistration() {
        return JsonRpcMethodRegistration.of("manual.echo", params -> StringNode.valueOf("echo"));
    }

    @Bean
    JsonRpcMethodRegistration typedUpperRegistration(JsonRpcTypedMethodHandlerFactory factory) {
        return JsonRpcMethodRegistration.of(
                "typed.upper",
                factory.unary(UpperInput.class, input -> new UpperOutput(input.value().toUpperCase()))
        );
    }

    @Bean
    JsonRpcMethodRegistration typedTagsRegistration(JsonRpcTypedMethodHandlerFactory factory) {
        return JsonRpcMethodRegistration.of(
                "typed.tags",
                factory.noParams(() -> List.of("alpha", "beta"))
        );
    }

    record UpperInput(String value) {
    }

    record UpperOutput(String value) {
    }
}
