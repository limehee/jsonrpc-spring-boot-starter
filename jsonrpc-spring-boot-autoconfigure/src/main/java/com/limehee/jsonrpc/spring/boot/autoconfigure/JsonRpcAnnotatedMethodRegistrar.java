package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodHandler;
import com.limehee.jsonrpc.core.JsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JsonRpcParam;
import com.limehee.jsonrpc.core.JsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

class JsonRpcAnnotatedMethodRegistrar implements SmartInitializingSingleton {

    private final ListableBeanFactory beanFactory;
    private final JsonRpcDispatcher dispatcher;
    private final JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory;
    private final JsonRpcParameterBinder parameterBinder;
    private final JsonRpcResultWriter resultWriter;

    JsonRpcAnnotatedMethodRegistrar(
            ListableBeanFactory beanFactory,
            JsonRpcDispatcher dispatcher,
            JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory,
            JsonRpcParameterBinder parameterBinder,
            JsonRpcResultWriter resultWriter
    ) {
        this.beanFactory = beanFactory;
        this.dispatcher = dispatcher;
        this.typedMethodHandlerFactory = typedMethodHandlerFactory;
        this.parameterBinder = parameterBinder;
        this.resultWriter = resultWriter;
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

        return params -> resultWriter.write(invoke(bean, method, bindMethodParams(method, params)));
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

    private Object[] bindMethodParams(Method method, com.fasterxml.jackson.databind.JsonNode params) {
        if (params != null && params.isObject()) {
            return bindNamedParams(method, params);
        }
        return bindPositionalParams(method, params);
    }

    private Object[] bindPositionalParams(Method method, com.fasterxml.jackson.databind.JsonNode params) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (params == null || !params.isArray() || params.size() != parameterTypes.length) {
            throw new IllegalArgumentException("Invalid params");
        }

        Object[] bound = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            bound[i] = parameterBinder.bind(params.get(i), parameterTypes[i]);
        }
        return bound;
    }

    private Object[] bindNamedParams(Method method, com.fasterxml.jackson.databind.JsonNode params) {
        Parameter[] parameters = method.getParameters();
        Object[] bound = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = resolveParameterName(parameter);
            com.fasterxml.jackson.databind.JsonNode valueNode = params.get(paramName);
            if (valueNode == null) {
                throw new IllegalArgumentException("Invalid params");
            }
            bound[i] = parameterBinder.bind(valueNode, parameter.getType());
        }
        return bound;
    }

    private String resolveParameterName(Parameter parameter) {
        JsonRpcParam annotation = parameter.getAnnotation(JsonRpcParam.class);
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }
        throw new IllegalArgumentException("Invalid params");
    }
}
