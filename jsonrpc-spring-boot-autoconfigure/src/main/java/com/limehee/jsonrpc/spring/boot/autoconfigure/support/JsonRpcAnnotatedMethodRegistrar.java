package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcConstants;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodHandler;
import com.limehee.jsonrpc.core.JsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JsonRpcParam;
import com.limehee.jsonrpc.core.JsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Registers methods annotated with {@link JsonRpcMethod} into the dispatcher after Spring
 * singleton initialization is complete.
 * <p>
 * Resolution rules:
 * </p>
 * <ul>
 * <li>If {@link JsonRpcMethod#value()} is blank, the Java method name is used as JSON-RPC method.</li>
 * <li>Methods with zero parameters are registered as no-parameter handlers.</li>
 * <li>Methods with one parameter are registered through unary typed binding.</li>
 * <li>Methods with multiple parameters support positional arrays and named-object binding.</li>
 * </ul>
 * <p>
 * For named binding, parameter names are resolved from {@link JsonRpcParam} first, then from
 * Java reflection metadata (requires {@code -parameters} compiler flag).
 * </p>
 */
public final class JsonRpcAnnotatedMethodRegistrar implements SmartInitializingSingleton {

    private final ListableBeanFactory beanFactory;
    private final JsonRpcDispatcher dispatcher;
    private final JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory;
    private final JsonRpcParameterBinder parameterBinder;
    private final JsonRpcResultWriter resultWriter;

    /**
     * Creates a registrar that scans beans and wires annotated methods into the dispatcher.
     *
     * @param beanFactory bean factory used to enumerate and resolve candidate beans
     * @param dispatcher dispatcher where resolved methods are registered
     * @param typedMethodHandlerFactory factory used for no-arg and unary handler creation
     * @param parameterBinder binder used for parameter conversion from JSON values
     * @param resultWriter writer used to serialize Java results into JSON nodes
     */
    public JsonRpcAnnotatedMethodRegistrar(
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

    /**
     * Scans initialized beans, finds {@link JsonRpcMethod} declarations, and registers handlers.
     *
     * @throws IllegalStateException if a bean cannot be resolved for scanning
     */
    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanType = beanFactory.getType(beanName, false);
            if (beanType == null) {
                continue;
            }

            Class<?> userClass = ClassUtils.getUserClass(beanType);
            List<Method> annotatedMethods = findAnnotatedMethods(userClass);
            if (annotatedMethods.isEmpty()) {
                continue;
            }

            Object bean;
            try {
                bean = beanFactory.getBean(beanName);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to initialize bean for @JsonRpcMethod scanning: " + beanName, ex);
            }

            for (Method method : annotatedMethods) {
                JsonRpcMethod annotation = method.getAnnotation(JsonRpcMethod.class);
                String methodName = annotation.value().isBlank() ? method.getName() : annotation.value();
                Method invocableMethod = resolveInvocableMethod(bean.getClass(), method);
                JsonRpcMethodHandler handler = buildHandler(bean, invocableMethod);
                dispatcher.register(methodName, handler);
            }
        }
    }

    /**
     * Collects public methods annotated with {@link JsonRpcMethod} from the user class.
     *
     * @param beanType user class to inspect
     * @return annotated methods discovered on the class
     */
    private List<Method> findAnnotatedMethods(Class<?> beanType) {
        Method[] methods = beanType.getMethods();
        List<Method> annotated = new ArrayList<>(methods.length);
        for (Method method : methods) {
            if (method.isAnnotationPresent(JsonRpcMethod.class)) {
                annotated.add(method);
            }
        }
        return annotated;
    }

    /**
     * Builds a method handler based on target method signature shape.
     *
     * @param bean bean instance declaring the method
     * @param method method to expose as JSON-RPC handler
     * @return handler that performs binding, invocation, and result serialization
     */
    private JsonRpcMethodHandler buildHandler(Object bean, Method method) {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(method, "method");
        makeInvocable(bean, method);

        int parameterCount = method.getParameterCount();
        if (parameterCount == 0) {
            return typedMethodHandlerFactory.noParams(() -> invoke(bean, method));
        }
        if (parameterCount == 1) {
            return unaryHandler(method.getParameterTypes()[0], bean, method);
        }

        return params -> resultWriter.write(invoke(bean, method, bindMethodParams(method, params)));
    }

    /**
     * Invokes the target method with prepared arguments.
     *
     * @param bean target bean
     * @param method method to invoke
     * @param args invocation arguments
     * @return invocation result
     * @throws IllegalStateException when reflection access is unexpectedly denied
     * @throws RuntimeException wrapping non-runtime target exceptions
     */
    private Object invoke(Object bean, Method method, Object... args) {
        try {
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

    /**
     * Creates a unary handler that binds one parameter and invokes the target method.
     *
     * @param paramType method parameter type
     * @param bean target bean
     * @param method target method
     * @param <T> static parameter type
     * @return unary JSON-RPC method handler
     */
    @SuppressWarnings("unchecked")
    private <T> JsonRpcMethodHandler unaryHandler(Class<?> paramType, Object bean, Method method) {
        return typedMethodHandlerFactory.unary((Class<T>) paramType, param -> invoke(bean, method, param));
    }

    /**
     * Binds request params to method arguments using named or positional strategy.
     *
     * @param method target method
     * @param params JSON-RPC {@code params} value
     * @return bound method arguments
     */
    private Object[] bindMethodParams(Method method, JsonNode params) {
        if (params != null && params.isObject()) {
            return bindNamedParams(method, params);
        }
        return bindPositionalParams(method, params);
    }

    /**
     * Binds positional array params to method parameters in index order.
     *
     * @param method target method
     * @param params JSON-RPC params node expected to be an array
     * @return bound arguments
     * @throws JsonRpcException when params are missing, not an array, or size does not match
     */
    private Object[] bindPositionalParams(Method method, JsonNode params) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (params == null || !params.isArray() || params.size() != parameterTypes.length) {
            throw invalidParamsException();
        }

        Object[] bound = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            bound[i] = parameterBinder.bind(params.get(i), parameterTypes[i]);
        }
        return bound;
    }

    /**
     * Binds named-object params to method parameters by resolved parameter names.
     *
     * @param method target method
     * @param params JSON-RPC params node expected to be an object
     * @return bound arguments
     * @throws JsonRpcException when a required named parameter is missing
     */
    private Object[] bindNamedParams(Method method, JsonNode params) {
        Parameter[] parameters = method.getParameters();
        Object[] bound = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            String paramName = resolveParameterName(parameter);
            JsonNode valueNode = params.get(paramName);
            if (valueNode == null) {
                throw invalidParamsException();
            }
            bound[i] = parameterBinder.bind(valueNode, parameter.getType());
        }
        return bound;
    }

    /**
     * Resolves effective parameter name for named binding.
     *
     * @param parameter target parameter metadata
     * @return resolved parameter name
     * @throws JsonRpcException when no explicit or reflective parameter name is available
     */
    private String resolveParameterName(Parameter parameter) {
        JsonRpcParam annotation = parameter.getAnnotation(JsonRpcParam.class);
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        if (parameter.isNamePresent()) {
            return parameter.getName();
        }
        throw invalidParamsException();
    }

    /**
     * Creates a standardized {@code INVALID_PARAMS} protocol exception.
     *
     * @return protocol exception for invalid parameter shape or binding
     */
    private JsonRpcException invalidParamsException() {
        return new JsonRpcException(JsonRpcErrorCode.INVALID_PARAMS, JsonRpcConstants.MESSAGE_INVALID_PARAMS);
    }

    /**
     * Resolves the invocable method from runtime bean type, handling potential proxies.
     *
     * @param beanClass runtime bean class
     * @param candidate method from user class
     * @return method instance invocable on the runtime bean
     */
    private Method resolveInvocableMethod(Class<?> beanClass, Method candidate) {
        try {
            return beanClass.getMethod(candidate.getName(), candidate.getParameterTypes());
        } catch (NoSuchMethodException ignored) {
            return candidate;
        }
    }

    /**
     * Makes the method accessible for reflective invocation when necessary.
     *
     * @param bean bean instance used for accessibility checks
     * @param method method to make invocable
     */
    private void makeInvocable(Object bean, Method method) {
        if (!method.canAccess(bean)) {
            method.setAccessible(true);
        }
    }
}
