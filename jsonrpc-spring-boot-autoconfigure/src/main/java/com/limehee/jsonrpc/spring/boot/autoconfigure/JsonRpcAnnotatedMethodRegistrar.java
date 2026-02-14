package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodHandler;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

class JsonRpcAnnotatedMethodRegistrar implements SmartInitializingSingleton {

    private final ListableBeanFactory beanFactory;
    private final JsonRpcDispatcher dispatcher;
    private final JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory;

    JsonRpcAnnotatedMethodRegistrar(
            ListableBeanFactory beanFactory,
            JsonRpcDispatcher dispatcher,
            JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory
    ) {
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
        this.typedMethodHandlerFactory = typedMethodHandlerFactory;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Object bean;
            try {
                bean = beanFactory.getBean(beanName);
            } catch (Exception ex) {
                continue;
            }

            for (Method method : bean.getClass().getMethods()) {
                JsonRpcMethod annotation = method.getAnnotation(JsonRpcMethod.class);
                if (annotation == null) {
                    continue;
                }

                String methodName = annotation.value().isBlank() ? method.getName() : annotation.value();
                JsonRpcMethodHandler handler = buildHandler(bean, method);
                dispatcher.register(methodName, handler);
            }
        }
    }

    private JsonRpcMethodHandler buildHandler(Object bean, Method method) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(method, "method");

        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            return typedMethodHandlerFactory.noParams(() -> invoke(bean, method));
        }
        if (parameterCount == 1) {
            return unaryHandler(method.getParameterTypes()[0], bean, method);
        }

        throw new IllegalStateException("@JsonRpcMethod supports only 0 or 1 parameter methods: " + method);
    }

    private Object invoke(Object bean, Method method, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(bean, args);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot access @JsonRpcMethod method: " + method, ex);
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            if (target instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(target);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> JsonRpcMethodHandler unaryHandler(Class<?> paramType, Object bean, Method method) {
        return typedMethodHandlerFactory.unary((Class<T>) paramType, param -> invoke(bean, method, param));
    }
}
