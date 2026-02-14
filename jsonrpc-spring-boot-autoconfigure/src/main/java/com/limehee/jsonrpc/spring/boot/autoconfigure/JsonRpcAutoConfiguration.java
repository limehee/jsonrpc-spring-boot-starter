package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.DefaultJsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JacksonJsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JacksonJsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.JsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.JsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequestParser;
import com.limehee.jsonrpc.core.JsonRpcRequestValidator;
import com.limehee.jsonrpc.core.JsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcResponseComposer;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.DirectJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.ExecutorJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcAnnotatedMethodRegistrar;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMethodAccessInterceptor;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import com.limehee.jsonrpc.spring.webmvc.DefaultJsonRpcHttpStatusStrategy;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcHttpStatusStrategy;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcEndpoint;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@AutoConfiguration
@EnableConfigurationProperties(JsonRpcProperties.class)
@ConditionalOnClass(JsonRpcDispatcher.class)
public class JsonRpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcMethodRegistry jsonRpcMethodRegistry(JsonRpcProperties properties) {
        return new InMemoryJsonRpcMethodRegistry(properties.getMethodRegistrationConflictPolicy());
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcRequestParser jsonRpcRequestParser() {
        return new DefaultJsonRpcRequestParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcRequestValidator jsonRpcRequestValidator() {
        return new DefaultJsonRpcRequestValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcMethodInvoker jsonRpcMethodInvoker() {
        return new DefaultJsonRpcMethodInvoker();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcExceptionResolver jsonRpcExceptionResolver(JsonRpcProperties properties) {
        return new DefaultJsonRpcExceptionResolver(properties.isIncludeErrorData());
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResponseComposer jsonRpcResponseComposer() {
        return new DefaultJsonRpcResponseComposer();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcParameterBinder jsonRpcParameterBinder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new JacksonJsonRpcParameterBinder(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResultWriter jsonRpcResultWriter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new JacksonJsonRpcResultWriter(objectMapperProvider.getIfAvailable(ObjectMapper::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcTypedMethodHandlerFactory jsonRpcTypedMethodHandlerFactory(
            JsonRpcParameterBinder parameterBinder,
            JsonRpcResultWriter resultWriter
    ) {
        return new DefaultJsonRpcTypedMethodHandlerFactory(parameterBinder, resultWriter);
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcNotificationExecutor jsonRpcNotificationExecutor(
            JsonRpcProperties properties,
            ObjectProvider<Executor> executors
    ) {
        if (properties.isNotificationExecutorEnabled()) {
            Executor executor = executors.orderedStream().findFirst().orElse(null);
            if (executor != null) {
                return new ExecutorJsonRpcNotificationExecutor(executor);
            }
        }
        return new DirectJsonRpcNotificationExecutor();
    }

    @Bean
    @ConditionalOnMissingBean(name = "jsonRpcMethodAccessInterceptor")
    public JsonRpcInterceptor jsonRpcMethodAccessInterceptor(JsonRpcProperties properties) {
        return new JsonRpcMethodAccessInterceptor(
                normalizeMethodSet(properties.getMethodAllowlist()),
                normalizeMethodSet(properties.getMethodDenylist())
        );
    }

    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(name = "jsonRpcMetricsInterceptor")
    @ConditionalOnProperty(prefix = "jsonrpc", name = "metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcInterceptor jsonRpcMetricsInterceptor(MeterRegistry meterRegistry) {
        return new JsonRpcMetricsInterceptor(meterRegistry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "jsonrpc", name = "scan-annotated-methods", havingValue = "true", matchIfMissing = true)
    public JsonRpcAnnotatedMethodRegistrar jsonRpcAnnotatedMethodRegistrar(
            ListableBeanFactory beanFactory,
            JsonRpcDispatcher dispatcher,
            JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory,
            JsonRpcParameterBinder parameterBinder,
            JsonRpcResultWriter resultWriter
    ) {
        return new JsonRpcAnnotatedMethodRegistrar(
                beanFactory,
                dispatcher,
                typedMethodHandlerFactory,
                parameterBinder,
                resultWriter
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcDispatcher jsonRpcDispatcher(
            JsonRpcMethodRegistry methodRegistry,
            JsonRpcRequestParser requestParser,
            JsonRpcRequestValidator requestValidator,
            JsonRpcMethodInvoker methodInvoker,
            JsonRpcExceptionResolver exceptionResolver,
            JsonRpcResponseComposer responseComposer,
            JsonRpcNotificationExecutor notificationExecutor,
            JsonRpcProperties properties,
            ObjectProvider<JsonRpcMethodRegistration> registrations,
            ObjectProvider<JsonRpcInterceptor> interceptors
    ) {
        validateProperties(properties);
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                methodRegistry,
                requestParser,
                requestValidator,
                methodInvoker,
                exceptionResolver,
                responseComposer,
                properties.getMaxBatchSize(),
                interceptors.orderedStream().toList(),
                notificationExecutor
        );
        registrations.orderedStream().forEach(registration ->
                dispatcher.register(registration.method(), registration.handler()));
        return dispatcher;
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcHttpStatusStrategy jsonRpcHttpStatusStrategy() {
        return new DefaultJsonRpcHttpStatusStrategy();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnBean(JsonRpcDispatcher.class)
    @ConditionalOnClass(JsonRpcWebMvcEndpoint.class)
    @ConditionalOnProperty(prefix = "jsonrpc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcWebMvcEndpoint jsonRpcWebMvcEndpoint(
            JsonRpcDispatcher dispatcher,
            JsonRpcHttpStatusStrategy httpStatusStrategy,
            ObjectProvider<ObjectMapper> objectMapperProvider,
            JsonRpcProperties properties
    ) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new JsonRpcWebMvcEndpoint(
                dispatcher,
                objectMapper,
                httpStatusStrategy,
                properties.getMaxRequestBytes()
        );
    }

    private Set<String> normalizeMethodSet(List<String> methods) {
        Set<String> normalized = new LinkedHashSet<>();
        if (methods == null) {
            return normalized;
        }
        for (String method : methods) {
            if (method == null) {
                continue;
            }
            String value = method.trim();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private void validateProperties(JsonRpcProperties properties) {
        if (properties.getPath() == null || properties.getPath().isBlank()) {
            throw new IllegalArgumentException("jsonrpc.path must not be blank");
        }
        if (!properties.getPath().startsWith("/") || containsWhitespace(properties.getPath())) {
            throw new IllegalArgumentException("jsonrpc.path must start with '/' and must not contain whitespace");
        }
        if (properties.getMaxBatchSize() <= 0) {
            throw new IllegalArgumentException("jsonrpc.max-batch-size must be greater than 0");
        }
        if (properties.getMaxRequestBytes() <= 0) {
            throw new IllegalArgumentException("jsonrpc.max-request-bytes must be greater than 0");
        }
        if (properties.getMethodRegistrationConflictPolicy() == null) {
            throw new IllegalArgumentException("jsonrpc.method-registration-conflict-policy must not be null");
        }

        validateMethodList("jsonrpc.method-allowlist", properties.getMethodAllowlist());
        validateMethodList("jsonrpc.method-denylist", properties.getMethodDenylist());
    }

    private boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void validateMethodList(String propertyName, List<String> methods) {
        if (methods == null) {
            throw new IllegalArgumentException(propertyName + " must not be null");
        }
        for (String method : methods) {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException(propertyName + " entries must not be blank");
            }
        }
    }
}
