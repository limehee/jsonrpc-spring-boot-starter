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
import com.limehee.jsonrpc.core.JsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequestParser;
import com.limehee.jsonrpc.core.JsonRpcRequestValidator;
import com.limehee.jsonrpc.core.JsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcResponseComposer;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
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

@AutoConfiguration
@EnableConfigurationProperties(JsonRpcProperties.class)
@ConditionalOnClass(JsonRpcDispatcher.class)
public class JsonRpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcMethodRegistry jsonRpcMethodRegistry(JsonRpcProperties properties) {
        return new InMemoryJsonRpcMethodRegistry(properties.getMethodNamespacePolicy());
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
            JsonRpcTypedMethodHandlerFactory typedMethodHandlerFactory
    ) {
        return new JsonRpcAnnotatedMethodRegistrar(beanFactory, dispatcher, typedMethodHandlerFactory);
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
            JsonRpcProperties properties,
            ObjectProvider<JsonRpcMethodRegistration> registrations,
            ObjectProvider<JsonRpcInterceptor> interceptors
    ) {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                methodRegistry,
                requestParser,
                requestValidator,
                methodInvoker,
                exceptionResolver,
                responseComposer,
                Math.max(1, properties.getMaxBatchSize()),
                interceptors.orderedStream().toList()
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
                Math.max(1, properties.getMaxRequestBytes())
        );
    }
}
