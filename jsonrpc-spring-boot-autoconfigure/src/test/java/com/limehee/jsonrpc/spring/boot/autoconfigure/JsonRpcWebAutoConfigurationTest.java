package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.spring.webmvc.JsonRpcHttpStatusStrategy;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void usesCustomHttpStatusStrategyBean() {
        webContextRunner
                .withUserConfiguration(CustomHttpStatusStrategyConfig.class)
                .run(context -> {
                    JsonRpcWebMvcEndpoint endpoint = context.getBean(JsonRpcWebMvcEndpoint.class);
                    HttpStatusCode status = endpoint.invoke("{".getBytes(StandardCharsets.UTF_8)).getStatusCode();
                    assertEquals(HttpStatus.BAD_REQUEST.value(), status.value());
                });
    }

    @Test
    void rejectsMaxRequestBytesLessThanOne() {
        webContextRunner
                .withPropertyValues("jsonrpc.max-request-bytes=0")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomHttpStatusStrategyConfig {
        @Bean
        JsonRpcHttpStatusStrategy customJsonRpcHttpStatusStrategy() {
            return new JsonRpcHttpStatusStrategy() {
                @Override
                public HttpStatus statusForSingle(com.limehee.jsonrpc.core.JsonRpcResponse response) {
                    return HttpStatus.OK;
                }

                @Override
                public HttpStatus statusForBatch(java.util.List<com.limehee.jsonrpc.core.JsonRpcResponse> responses) {
                    return HttpStatus.OK;
                }

                @Override
                public HttpStatus statusForNotificationOnly() {
                    return HttpStatus.NO_CONTENT;
                }

                @Override
                public HttpStatus statusForParseError() {
                    return HttpStatus.BAD_REQUEST;
                }

                @Override
                public HttpStatus statusForRequestTooLarge() {
                    return HttpStatus.valueOf(413);
                }
            };
        }
    }
}
