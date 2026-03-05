package com.limehee.jsonrpc.spring.boot.autoconfigure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.DefaultJsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.DirectJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JacksonJsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JacksonJsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcAnnotatedMethodRegistrar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.IntNode;

class JsonRpcAnnotatedMethodRegistrarTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void failsFastWhenAnnotatedBeanCannotBeCreated() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("failingBean", new RootBeanDefinition(FailingAnnotatedBean.class));

        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        JsonRpcAnnotatedMethodRegistrar registrar = registrar(beanFactory, dispatcher);

        IllegalStateException exception = assertThrows(IllegalStateException.class, registrar::afterSingletonsInstantiated);
        assertTrue(exception.getMessage().contains("failingBean"));
    }

    @Test
    void wrapsCheckedTargetExceptionFromAnnotatedMethodInvocation() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerBeanDefinition("checkedBean", new RootBeanDefinition(CheckedExceptionAnnotatedBean.class));

        AtomicReference<Throwable> captured = new AtomicReference<>();
        JsonRpcExceptionResolver exceptionResolver = throwable -> {
            captured.set(throwable);
            return new DefaultJsonRpcExceptionResolver(false).resolve(throwable);
        };

        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            exceptionResolver,
            new DefaultJsonRpcResponseComposer(),
            100,
            List.of(),
            new DirectJsonRpcNotificationExecutor()
        );

        JsonRpcAnnotatedMethodRegistrar registrar = registrar(beanFactory, dispatcher);
        registrar.afterSingletonsInstantiated();

        JsonRpcResponse response = dispatcher.dispatch(
            new JsonRpcRequest("2.0", IntNode.valueOf(1), "checked.fail", null, true)
        );

        assertNotNull(response);
        assertNotNull(response.error());
        assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, response.error().code());
        assertNotNull(captured.get());
        assertTrue(captured.get() instanceof RuntimeException);
        assertNotNull(captured.get().getCause());
        assertEquals(Exception.class, captured.get().getCause().getClass());
        assertEquals("checked failure", captured.get().getCause().getMessage());
    }

    private JsonRpcAnnotatedMethodRegistrar registrar(DefaultListableBeanFactory beanFactory, JsonRpcDispatcher dispatcher) {
        JacksonJsonRpcParameterBinder parameterBinder = new JacksonJsonRpcParameterBinder(OBJECT_MAPPER);
        JacksonJsonRpcResultWriter resultWriter = new JacksonJsonRpcResultWriter(OBJECT_MAPPER);
        JsonRpcTypedMethodHandlerFactory typedFactory = new DefaultJsonRpcTypedMethodHandlerFactory(
            parameterBinder,
            resultWriter
        );
        return new JsonRpcAnnotatedMethodRegistrar(
            beanFactory,
            dispatcher,
            typedFactory,
            parameterBinder,
            resultWriter
        );
    }

    static class FailingAnnotatedBean {

        FailingAnnotatedBean() {
            throw new IllegalStateException("creation failed");
        }

        @JsonRpcMethod("never")
        public String never() {
            return "never";
        }
    }

    static class CheckedExceptionAnnotatedBean {

        @JsonRpcMethod("checked.fail")
        public String fail() throws Exception {
            throw new Exception("checked failure");
        }
    }
}
