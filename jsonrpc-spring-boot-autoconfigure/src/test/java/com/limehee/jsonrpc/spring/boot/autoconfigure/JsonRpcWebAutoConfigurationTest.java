package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRpcWebAutoConfigurationTest {

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JsonRpcAutoConfiguration.class));

    @Test
    void createsWebMvcEndpointWhenEnabled() {
        webContextRunner.run(context ->
                assertTrue(context.containsBean("jsonRpcWebMvcEndpoint")));
    }

    @Test
    void doesNotCreateWebMvcEndpointWhenDisabled() {
        webContextRunner
                .withPropertyValues("jsonrpc.enabled=false")
                .run(context -> assertFalse(context.containsBean("jsonRpcWebMvcEndpoint")));
    }

    @Test
    void exposesJsonRpcWebMvcEndpointType() {
        webContextRunner.run(context ->
                assertTrue(context.getBean("jsonRpcWebMvcEndpoint") instanceof JsonRpcWebMvcEndpoint));
    }
}
