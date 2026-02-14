package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcParam;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    void doesNotRegisterAnnotatedMethodsWhenScanDisabled() throws Exception {
        contextRunner
                .withPropertyValues("jsonrpc.scan-annotated-methods=false")
                .withUserConfiguration(AnnotatedMethodConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(70),
                            "hello",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"name\":\"codex\"}"),
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32601, response.error().code());
                });
    }

    @Test
    void registersAnnotatedMethodsWithPositionalParams() throws Exception {
        contextRunner
                .withUserConfiguration(AnnotatedPositionalMethodConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    assertNotNull(dispatcher);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(8),
                            "sum",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("[2,3]"),
                            true
                    ));

                    assertNotNull(response);
                    assertEquals(5, response.result().asInt());
                });
    }

    @Test
    void positionalAnnotatedMethodReturnsInvalidParamsWhenRequiredFieldMissing() throws Exception {
        contextRunner
                .withUserConfiguration(AnnotatedPositionalMethodConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(9),
                            "sum",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"left\":2}"),
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32602, response.error().code());
                });
    }

    @Test
    void registersAnnotatedMethodsWithNamedParamsObject() throws Exception {
        contextRunner
                .withUserConfiguration(AnnotatedNamedMethodConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    assertNotNull(dispatcher);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(10),
                            "concat",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"left\":\"a\",\"right\":\"b\"}"),
                            true
                    ));

                    assertNotNull(response);
                    assertEquals("ab", response.result().asText());
                });
    }

    @Test
    void namedAnnotatedMethodReturnsInvalidParamsWhenFieldMissing() throws Exception {
        contextRunner
                .withUserConfiguration(AnnotatedNamedMethodConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(12),
                            "concat",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"left\":\"a\"}"),
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32602, response.error().code());
                });
    }

    @Test
    void namedAnnotatedMethodCanUseJavaParameterNamesWhenAvailable() throws Exception {
        contextRunner
                .withUserConfiguration(AnnotatedNamedWithoutParamAnnotationConfig.class)
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(13),
                            "join",
                            new com.fasterxml.jackson.databind.ObjectMapper().readTree("{\"left\":\"x\",\"right\":\"y\"}"),
                            true
                    ));

                    assertNotNull(response);
                    assertEquals("xy", response.result().asText());
                });
    }

    @Test
    void wiresInterceptorsIntoDispatcher() {
        contextRunner
                .withUserConfiguration(InterceptorConfig.class)
                .withBean("ping", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    CountingInterceptor interceptor = context.getBean(CountingInterceptor.class);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(1),
                            "ping",
                            null,
                            true
                    ));

                    assertEquals("pong", response.result().asText());
                    assertTrue(interceptor.beforeInvokeCount > 0);
                    assertTrue(interceptor.afterInvokeCount > 0);
                });
    }

    @Test
    void hidesErrorDataByDefault() {
        contextRunner
                .withBean("boom", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("boom", params -> {
                            throw new JsonRpcException(-32001, "domain", TextNode.valueOf("secret"));
                        }))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(11),
                            "boom",
                            null,
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32001, response.error().code());
                    assertEquals("domain", response.error().message());
                    assertNull(response.error().data());
                });
    }

    @Test
    void includesErrorDataWhenConfigured() {
        contextRunner
                .withPropertyValues("jsonrpc.include-error-data=true")
                .withBean("boom", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("boom", params -> {
                            throw new JsonRpcException(-32001, "domain", TextNode.valueOf("secret"));
                        }))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(11),
                            "boom",
                            null,
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32001, response.error().code());
                    assertEquals("secret", response.error().data().asText());
                });
    }

    @Test
    void recordsMetricsWhenEnabled() {
        contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean("ping", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);

                    dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(21),
                            "ping",
                            null,
                            true
                    ));

                    double callCount = meterRegistry.counter(
                            "jsonrpc.server.calls",
                            "method", "ping",
                            "outcome", "success",
                            "errorCode", "none"
                    ).count();
                    assertEquals(1.0, callCount);
                });
    }

    @Test
    void doesNotCreateMetricsInterceptorWhenDisabled() {
        contextRunner
                .withPropertyValues("jsonrpc.metrics-enabled=false")
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertFalse(context.containsBean("jsonRpcMetricsInterceptor")));
    }

    @Test
    void blocksMethodsNotInAllowlist() {
        contextRunner
                .withPropertyValues("jsonrpc.method-allowlist[0]=ping")
                .withBean("pong", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("pong", params -> TextNode.valueOf("pong")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(31),
                            "pong",
                            null,
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32601, response.error().code());
                });
    }

    @Test
    void blocksMethodsInDenylist() {
        contextRunner
                .withPropertyValues("jsonrpc.method-denylist[0]=ping")
                .withBean("ping", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);

                    JsonRpcResponse response = dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            IntNode.valueOf(32),
                            "ping",
                            null,
                            true
                    ));

                    assertNotNull(response.error());
                    assertEquals(-32601, response.error().code());
                });
    }

    @Test
    void duplicateMethodRegistrationFailsByDefault() {
        contextRunner
                .withBean("ping1", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong1")))
                .withBean("ping2", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong2")))
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    void duplicateMethodRegistrationCanBeReplacedWhenConfigured() {
        contextRunner
                .withPropertyValues("jsonrpc.method-registration-conflict-policy=REPLACE")
                .withBean("ping1", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong1")))
                .withBean("ping2", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong2")))
                .run(context -> assertNull(context.getStartupFailure()));
    }

    @Test
    void usesExecutorForNotificationsWhenEnabled() {
        contextRunner
                .withPropertyValues("jsonrpc.notification-executor-enabled=true")
                .withUserConfiguration(NotificationExecutorConfig.class)
                .withBean("notify", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("notify", params -> TextNode.valueOf("ok")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    CountingExecutor executor = context.getBean(CountingExecutor.class);

                    dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            null,
                            "notify",
                            null,
                            false
                    ));

                    assertEquals(1, executor.executeCount.get());
                });
    }

    @Test
    void doesNotUseExecutorForNotificationsWhenDisabled() {
        contextRunner
                .withPropertyValues("jsonrpc.notification-executor-enabled=false")
                .withUserConfiguration(NotificationExecutorConfig.class)
                .withBean("notify", JsonRpcMethodRegistration.class,
                        () -> JsonRpcMethodRegistration.of("notify", params -> TextNode.valueOf("ok")))
                .run(context -> {
                    JsonRpcDispatcher dispatcher = context.getBean(JsonRpcDispatcher.class);
                    CountingExecutor executor = context.getBean(CountingExecutor.class);

                    dispatcher.dispatch(new JsonRpcRequest(
                            "2.0",
                            null,
                            "notify",
                            null,
                            false
                    ));

                    assertEquals(0, executor.executeCount.get());
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedMethodConfig {
        @Bean
        AnnotatedHandler annotatedHandler() {
            return new AnnotatedHandler();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedPositionalMethodConfig {
        @Bean
        AnnotatedPositionalHandler annotatedPositionalHandler() {
            return new AnnotatedPositionalHandler();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedNamedMethodConfig {
        @Bean
        AnnotatedNamedHandler annotatedNamedHandler() {
            return new AnnotatedNamedHandler();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AnnotatedNamedWithoutParamAnnotationConfig {
        @Bean
        AnnotatedNamedWithoutParamAnnotationHandler annotatedNamedWithoutParamAnnotationHandler() {
            return new AnnotatedNamedWithoutParamAnnotationHandler();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class InterceptorConfig {
        @Bean
        CountingInterceptor countingInterceptor() {
            return new CountingInterceptor();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class NotificationExecutorConfig {
        @Bean
        CountingExecutor countingExecutor() {
            return new CountingExecutor();
        }
    }

    static class AnnotatedHandler {
        @JsonRpcMethod("hello")
        public String hello(NameParams params) {
            return "hello " + params.name();
        }
    }

    static class AnnotatedPositionalHandler {
        @JsonRpcMethod("sum")
        public int sum(int left, int right) {
            return left + right;
        }
    }

    static class AnnotatedNamedHandler {
        @JsonRpcMethod("concat")
        public String concat(@JsonRpcParam("left") String left, @JsonRpcParam("right") String right) {
            return left + right;
        }
    }

    static class AnnotatedNamedWithoutParamAnnotationHandler {
        @JsonRpcMethod("join")
        public String join(String left, String right) {
            return left + right;
        }
    }

    record NameParams(String name) {
    }

    static class CountingInterceptor implements JsonRpcInterceptor {
        int beforeInvokeCount;
        int afterInvokeCount;

        @Override
        public void beforeInvoke(JsonRpcRequest request) {
            beforeInvokeCount++;
        }

        @Override
        public void afterInvoke(JsonRpcRequest request, com.fasterxml.jackson.databind.JsonNode result) {
            afterInvokeCount++;
        }
    }

    static class CountingExecutor implements Executor {
        private final AtomicInteger executeCount = new AtomicInteger();

        @Override
        public void execute(Runnable command) {
            executeCount.incrementAndGet();
            command.run();
        }
    }
}
