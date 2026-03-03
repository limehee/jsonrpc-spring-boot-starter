package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CoreConstructorNullGuardTest {

    @Test
    void dispatcherConstructorRejectsNullMethodRegistry() {
        assertThrows(
                NullPointerException.class,
                () -> new JsonRpcDispatcher(
                        null,
                        new DefaultJsonRpcRequestParser(),
                        new DefaultJsonRpcRequestValidator(),
                        new DefaultJsonRpcMethodInvoker(),
                        new DefaultJsonRpcExceptionResolver(),
                        new DefaultJsonRpcResponseComposer(),
                        100,
                        List.of(),
                        new DirectJsonRpcNotificationExecutor()
                )
        );
    }

    @Test
    void dispatcherConstructorRejectsNullInterceptors() {
        assertThrows(
                NullPointerException.class,
                () -> new JsonRpcDispatcher(
                        new InMemoryJsonRpcMethodRegistry(),
                        new DefaultJsonRpcRequestParser(),
                        new DefaultJsonRpcRequestValidator(),
                        new DefaultJsonRpcMethodInvoker(),
                        new DefaultJsonRpcExceptionResolver(),
                        new DefaultJsonRpcResponseComposer(),
                        100,
                        null,
                        new DirectJsonRpcNotificationExecutor()
                )
        );
    }

    @Test
    void parameterBinderConstructorRejectsNullObjectMapper() {
        assertThrows(NullPointerException.class, () -> new JacksonJsonRpcParameterBinder(null));
    }

    @Test
    void typedMethodHandlerFactoryConstructorRejectsNullDependencies() {
        assertThrows(
                NullPointerException.class,
                () -> new DefaultJsonRpcTypedMethodHandlerFactory(null, value -> StringNode.valueOf("ok"))
        );
        assertThrows(
                NullPointerException.class,
                () -> new DefaultJsonRpcTypedMethodHandlerFactory(
                        new JsonRpcParameterBinder() {
                            @Override
                            public <T> T bind(JsonNode params, Class<T> targetType) {
                                return null;
                            }
                        },
                        null
                )
        );
    }

    @Test
    void executorNotificationConstructorRejectsNullExecutor() {
        assertThrows(NullPointerException.class, () -> new ExecutorJsonRpcNotificationExecutor(null));
    }
}
