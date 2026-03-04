package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.DirectJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.ExecutorJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JacksonJsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JacksonJsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.JsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JsonRpcRequestParser;
import com.limehee.jsonrpc.core.JsonRpcRequestValidationOptions;
import com.limehee.jsonrpc.core.JsonRpcRequestValidator;
import com.limehee.jsonrpc.core.JsonRpcResponseErrorCodePolicy;
import com.limehee.jsonrpc.core.JsonRpcResponseComposer;
import com.limehee.jsonrpc.core.JsonRpcResponseParser;
import com.limehee.jsonrpc.core.JsonRpcResponseValidationOptions;
import com.limehee.jsonrpc.core.JsonRpcResponseValidator;
import com.limehee.jsonrpc.core.JsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.InstrumentedJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcAnnotatedMethodRegistrar;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMethodAccessInterceptor;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMetricsInterceptor;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcWebMvcMetricsObserver;
import com.limehee.jsonrpc.spring.webmvc.DefaultJsonRpcHttpStatusStrategy;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcHttpStatusStrategy;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcEndpoint;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcWebMvcObserver;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.jspecify.annotations.Nullable;
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
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring Boot auto-configuration for JSON-RPC server components.
 * <p>
 * This configuration wires core dispatcher components, optional metrics integration, annotation scanning, transport
 * endpoint registration, and basic property validation.
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(JsonRpcProperties.class)
@ConditionalOnClass(JsonRpcDispatcher.class)
public class JsonRpcAutoConfiguration {

    /**
     * Creates the JSON-RPC method registry.
     *
     * @param properties bound JSON-RPC properties
     * @return method registry honoring configured conflict policy
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcMethodRegistry jsonRpcMethodRegistry(JsonRpcProperties properties) {
        return new InMemoryJsonRpcMethodRegistry(properties.getMethodRegistrationConflictPolicy());
    }

    /**
     * Creates the request parser used by the dispatcher.
     *
     * @return JSON-RPC request parser
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcRequestParser jsonRpcRequestParser() {
        return new DefaultJsonRpcRequestParser();
    }

    /**
     * Creates request-validation options bound from external configuration.
     *
     * @param properties bound JSON-RPC properties
     * @return request-validation options
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcRequestValidationOptions jsonRpcRequestValidationOptions(JsonRpcProperties properties) {
        JsonRpcProperties.Validation validation = properties.getValidation();
        if (validation == null) {
            throw new IllegalArgumentException("jsonrpc.validation must not be null");
        }
        JsonRpcProperties.Validation.Request request = validation.getRequest();
        if (request == null) {
            throw new IllegalArgumentException("jsonrpc.validation.request must not be null");
        }
        if (request.getParamsTypeViolationCodePolicy() == null) {
            throw new IllegalArgumentException(
                "jsonrpc.validation.request.params-type-violation-code-policy must not be null"
            );
        }
        return JsonRpcRequestValidationOptions.builder()
            .requireJsonRpcVersion20(request.isRequireJsonRpcVersion20())
            .requireIdMember(request.isRequireIdMember())
            .allowNullId(request.isAllowNullId())
            .allowStringId(request.isAllowStringId())
            .allowNumericId(request.isAllowNumericId())
            .allowFractionalId(request.isAllowFractionalId())
            .rejectResponseFields(request.isRejectResponseFields())
            .paramsTypeViolationCodePolicy(request.getParamsTypeViolationCodePolicy())
            .build();
    }

    /**
     * Creates request validator for JSON-RPC structural checks.
     *
     * @param options request-validation options
     * @return request validator
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcRequestValidator jsonRpcRequestValidator(JsonRpcRequestValidationOptions options) {
        return new DefaultJsonRpcRequestValidator(options);
    }

    /**
     * Creates response-validation options bound from external configuration.
     *
     * @param properties bound JSON-RPC properties
     * @return response-validation options
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResponseValidationOptions jsonRpcResponseValidationOptions(JsonRpcProperties properties) {
        JsonRpcProperties.Validation validation = properties.getValidation();
        if (validation == null) {
            throw new IllegalArgumentException("jsonrpc.validation must not be null");
        }
        JsonRpcProperties.Validation.Response response = validation.getResponse();
        if (response == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response must not be null");
        }
        JsonRpcProperties.Validation.Response.ErrorCode errorCode = response.getErrorCode();
        if (errorCode == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response.error-code must not be null");
        }
        if (errorCode.getPolicy() == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response.error-code.policy must not be null");
        }
        JsonRpcProperties.Validation.Response.ErrorCode.Range range = errorCode.getRange();
        if (range == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response.error-code.range must not be null");
        }
        return JsonRpcResponseValidationOptions.builder()
            .requireJsonRpcVersion20(response.isRequireJsonRpcVersion20())
            .requireIdMember(response.isRequireIdMember())
            .allowNullId(response.isAllowNullId())
            .allowStringId(response.isAllowStringId())
            .allowNumericId(response.isAllowNumericId())
            .allowFractionalId(response.isAllowFractionalId())
            .requireExclusiveResultOrError(response.isRequireExclusiveResultOrError())
            .requireErrorObjectWhenPresent(response.isRequireErrorObjectWhenPresent())
            .requireIntegerErrorCode(response.isRequireIntegerErrorCode())
            .requireStringErrorMessage(response.isRequireStringErrorMessage())
            .rejectRequestFields(response.isRejectRequestFields())
            .rejectDuplicateMembers(response.isRejectDuplicateMembers())
            .errorCodePolicy(errorCode.getPolicy())
            .errorCodeRangeMin(range.getMin())
            .errorCodeRangeMax(range.getMax())
            .build();
    }

    /**
     * Creates parser for incoming JSON-RPC response envelopes.
     *
     * @param objectMapperProvider provider for custom or default {@link ObjectMapper}
     * @param properties           bound JSON-RPC properties
     * @return response parser
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResponseParser jsonRpcResponseParser(
        ObjectProvider<ObjectMapper> objectMapperProvider,
        JsonRpcProperties properties
    ) {
        return new DefaultJsonRpcResponseParser(
            objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().build()),
            properties.getValidation().getResponse().isRejectDuplicateMembers()
        );
    }

    /**
     * Creates response validator for incoming JSON-RPC response envelopes.
     *
     * @param options response-validation options
     * @return response validator
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResponseValidator jsonRpcResponseValidator(JsonRpcResponseValidationOptions options) {
        return new DefaultJsonRpcResponseValidator(
            options
        );
    }

    /**
     * Creates method invoker used to execute registered handlers.
     *
     * @return method invoker implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcMethodInvoker jsonRpcMethodInvoker() {
        return new DefaultJsonRpcMethodInvoker();
    }

    /**
     * Creates exception resolver used to map failures to JSON-RPC errors.
     *
     * @param properties bound JSON-RPC properties
     * @return exception resolver configured with error-data inclusion policy
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcExceptionResolver jsonRpcExceptionResolver(JsonRpcProperties properties) {
        return new DefaultJsonRpcExceptionResolver(properties.isIncludeErrorData());
    }

    /**
     * Creates response composer used for successful and error responses.
     *
     * @return response composer implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResponseComposer jsonRpcResponseComposer() {
        return new DefaultJsonRpcResponseComposer();
    }

    /**
     * Creates parameter binder backed by Jackson.
     *
     * @param objectMapperProvider provider for custom or default {@link ObjectMapper}
     * @return parameter binder implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcParameterBinder jsonRpcParameterBinder(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new JacksonJsonRpcParameterBinder(
            objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    /**
     * Creates result writer backed by Jackson.
     *
     * @param objectMapperProvider provider for custom or default {@link ObjectMapper}
     * @return result writer implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcResultWriter jsonRpcResultWriter(ObjectProvider<ObjectMapper> objectMapperProvider) {
        return new JacksonJsonRpcResultWriter(objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().build()));
    }

    /**
     * Creates typed method handler factory used by typed registration utilities.
     *
     * @param parameterBinder binder used for parameter conversion
     * @param resultWriter    writer used for serializing handler results
     * @return typed method handler factory
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcTypedMethodHandlerFactory jsonRpcTypedMethodHandlerFactory(
        JsonRpcParameterBinder parameterBinder,
        JsonRpcResultWriter resultWriter
    ) {
        return new DefaultJsonRpcTypedMethodHandlerFactory(parameterBinder, resultWriter);
    }

    /**
     * Creates notification executor according to configuration and available executor beans.
     *
     * @param properties            bound JSON-RPC properties
     * @param beanFactory           bean factory used to discover candidate executors
     * @param meterRegistryProvider optional meter registry for executor instrumentation
     * @return notification executor implementation
     * @throws IllegalStateException if a configured executor bean name does not exist
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcNotificationExecutor jsonRpcNotificationExecutor(
        JsonRpcProperties properties,
        ListableBeanFactory beanFactory,
        ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        JsonRpcNotificationExecutor executor;
        if (!properties.isNotificationExecutorEnabled()) {
            executor = new DirectJsonRpcNotificationExecutor();
            return instrumentNotificationExecutorIfEnabled(executor, properties, meterRegistryProvider);
        }

        Map<String, Executor> executors = beanFactory.getBeansOfType(Executor.class, false, false);
        String configuredBeanName = trimToNull(properties.getNotificationExecutorBeanName());
        if (configuredBeanName != null) {
            Executor configuredExecutor = executors.get(configuredBeanName);
            if (configuredExecutor == null) {
                throw new IllegalStateException(
                    "jsonrpc.notification-executor-bean-name points to missing Executor bean: " + configuredBeanName);
            }
            executor = new ExecutorJsonRpcNotificationExecutor(configuredExecutor);
            return instrumentNotificationExecutorIfEnabled(executor, properties, meterRegistryProvider);
        }

        if (executors.size() == 1) {
            executor = new ExecutorJsonRpcNotificationExecutor(executors.values().iterator().next());
            return instrumentNotificationExecutorIfEnabled(executor, properties, meterRegistryProvider);
        }

        Executor applicationTaskExecutor = executors.get("applicationTaskExecutor");
        if (applicationTaskExecutor != null) {
            executor = new ExecutorJsonRpcNotificationExecutor(applicationTaskExecutor);
            return instrumentNotificationExecutorIfEnabled(executor, properties, meterRegistryProvider);
        }
        executor = new DirectJsonRpcNotificationExecutor();
        return instrumentNotificationExecutorIfEnabled(executor, properties, meterRegistryProvider);
    }

    /**
     * Creates method access control interceptor using configured allowlist and denylist.
     *
     * @param properties bound JSON-RPC properties
     * @return access control interceptor
     */
    @Bean
    @ConditionalOnMissingBean(name = "jsonRpcMethodAccessInterceptor")
    public JsonRpcInterceptor jsonRpcMethodAccessInterceptor(JsonRpcProperties properties) {
        return new JsonRpcMethodAccessInterceptor(
            normalizeMethodSet(properties.getMethodAllowlist()),
            normalizeMethodSet(properties.getMethodDenylist())
        );
    }

    /**
     * Creates Micrometer metrics interceptor for dispatcher lifecycle metrics.
     *
     * @param properties    bound JSON-RPC properties
     * @param meterRegistry meter registry used for metric publication
     * @return metrics interceptor
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(name = "jsonRpcMetricsInterceptor")
    @ConditionalOnProperty(prefix = "jsonrpc", name = "metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcInterceptor jsonRpcMetricsInterceptor(JsonRpcProperties properties, MeterRegistry meterRegistry) {
        return new JsonRpcMetricsInterceptor(
            meterRegistry,
            properties.isMetricsLatencyHistogramEnabled(),
            toPercentileArray(properties.getMetricsLatencyPercentiles()),
            properties.getMetricsMaxMethodTagValues()
        );
    }

    /**
     * Creates transport metrics observer for WebMVC-specific events.
     *
     * @param properties    bound JSON-RPC properties
     * @param meterRegistry meter registry used for metric publication
     * @return WebMVC metrics observer
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(JsonRpcWebMvcObserver.class)
    @ConditionalOnProperty(prefix = "jsonrpc", name = "metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcWebMvcObserver jsonRpcWebMvcMetricsObserver(JsonRpcProperties properties,
        MeterRegistry meterRegistry) {
        return new JsonRpcWebMvcMetricsObserver(
            meterRegistry,
            properties.isMetricsLatencyHistogramEnabled(),
            toPercentileArray(properties.getMetricsLatencyPercentiles())
        );
    }

    /**
     * Creates no-op WebMVC observer when no explicit observer bean is provided.
     *
     * @return no-op observer instance
     */
    @Bean
    @ConditionalOnMissingBean(JsonRpcWebMvcObserver.class)
    public JsonRpcWebMvcObserver jsonRpcWebMvcObserver() {
        return JsonRpcWebMvcObserver.noOp();
    }

    /**
     * Creates registrar that scans and registers {@link com.limehee.jsonrpc.core.JsonRpcMethod} annotated Spring bean
     * methods.
     *
     * @param beanFactory               Spring bean factory
     * @param dispatcher                dispatcher receiving discovered registrations
     * @param typedMethodHandlerFactory typed handler factory
     * @param parameterBinder           parameter binder
     * @param resultWriter              result writer
     * @return annotated method registrar
     */
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

    /**
     * Creates the JSON-RPC dispatcher and applies additional method registrations.
     *
     * @param methodRegistry       method registry
     * @param requestParser        request parser
     * @param requestValidator     request validator
     * @param methodInvoker        method invoker
     * @param exceptionResolver    exception resolver
     * @param responseComposer     response composer
     * @param notificationExecutor notification executor
     * @param properties           bound JSON-RPC properties
     * @param registrations        additional static method registrations
     * @param interceptors         dispatcher interceptors
     * @return configured dispatcher instance
     */
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

    /**
     * Creates default HTTP status mapping strategy.
     *
     * @return default HTTP status strategy
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonRpcHttpStatusStrategy jsonRpcHttpStatusStrategy() {
        return new DefaultJsonRpcHttpStatusStrategy();
    }

    /**
     * Creates JSON-RPC WebMVC endpoint for servlet applications.
     *
     * @param dispatcher           dispatcher handling JSON-RPC requests
     * @param httpStatusStrategy   strategy mapping protocol outcomes to HTTP status codes
     * @param objectMapperProvider provider for custom or default {@link ObjectMapper}
     * @param webMvcObserver       observer for transport-level events
     * @param properties           bound JSON-RPC properties
     * @return WebMVC endpoint bean
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnBean(JsonRpcDispatcher.class)
    @ConditionalOnClass(JsonRpcWebMvcEndpoint.class)
    @ConditionalOnProperty(prefix = "jsonrpc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcWebMvcEndpoint jsonRpcWebMvcEndpoint(
        JsonRpcDispatcher dispatcher,
        JsonRpcHttpStatusStrategy httpStatusStrategy,
        ObjectProvider<ObjectMapper> objectMapperProvider,
        JsonRpcWebMvcObserver webMvcObserver,
        JsonRpcProperties properties
    ) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(() -> JsonMapper.builder().build());
        return new JsonRpcWebMvcEndpoint(
            dispatcher,
            objectMapper,
            httpStatusStrategy,
            properties.getMaxRequestBytes(),
            webMvcObserver,
            properties.getValidation().getRequest().isRejectDuplicateMembers()
        );
    }

    /**
     * Normalizes method names by trimming blanks, removing nulls, and preserving insertion order.
     *
     * @param methods raw method name list
     * @return normalized method set
     */
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

    /**
     * Validates critical JSON-RPC property values before dispatcher creation.
     *
     * @param properties bound JSON-RPC properties
     * @throws IllegalArgumentException if one or more properties are invalid
     */
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
        if (properties.getMetricsMaxMethodTagValues() <= 0) {
            throw new IllegalArgumentException("jsonrpc.metrics-max-method-tag-values must be greater than 0");
        }
        if (properties.getNotificationExecutorBeanName() == null) {
            throw new IllegalArgumentException("jsonrpc.notification-executor-bean-name must not be null");
        }
        if (properties.getValidation() == null) {
            throw new IllegalArgumentException("jsonrpc.validation must not be null");
        }
        if (properties.getValidation().getRequest() == null) {
            throw new IllegalArgumentException("jsonrpc.validation.request must not be null");
        }
        if (properties.getValidation().getRequest().getParamsTypeViolationCodePolicy() == null) {
            throw new IllegalArgumentException(
                "jsonrpc.validation.request.params-type-violation-code-policy must not be null"
            );
        }
        if (properties.getValidation().getResponse() == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response must not be null");
        }
        if (properties.getValidation().getResponse().getErrorCode() == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response.error-code must not be null");
        }
        if (properties.getValidation().getResponse().getErrorCode().getPolicy() == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response.error-code.policy must not be null");
        }
        if (properties.getValidation().getResponse().getErrorCode().getRange() == null) {
            throw new IllegalArgumentException("jsonrpc.validation.response.error-code.range must not be null");
        }
        JsonRpcResponseErrorCodePolicy errorCodePolicy = properties.getValidation().getResponse().getErrorCode().getPolicy();
        Integer errorCodeMin = properties.getValidation().getResponse().getErrorCode().getRange().getMin();
        Integer errorCodeMax = properties.getValidation().getResponse().getErrorCode().getRange().getMax();
        if (!properties.getValidation().getResponse().isRequireIntegerErrorCode()
            && errorCodePolicy != JsonRpcResponseErrorCodePolicy.ANY_INTEGER) {
            throw new IllegalArgumentException(
                "jsonrpc.validation.response.error-code.policy requires jsonrpc.validation.response.require-integer-error-code=true"
            );
        }
        if (errorCodePolicy == JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE) {
            if (errorCodeMin == null || errorCodeMax == null) {
                throw new IllegalArgumentException(
                    "jsonrpc.validation.response.error-code.range.min and range.max are required for CUSTOM_RANGE"
                );
            }
            if (errorCodeMin > errorCodeMax) {
                throw new IllegalArgumentException(
                    "jsonrpc.validation.response.error-code.range.min must be less than or equal to range.max"
                );
            }
        }

        validateMethodList("jsonrpc.method-allowlist", properties.getMethodAllowlist());
        validateMethodList("jsonrpc.method-denylist", properties.getMethodDenylist());
        validatePercentiles(properties.getMetricsLatencyPercentiles());
    }

    /**
     * Checks whether a string contains any whitespace character.
     *
     * @param value value to inspect
     * @return {@code true} when value contains whitespace
     */
    private boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates method allow/deny list entries.
     *
     * @param propertyName validated property name for exception messages
     * @param methods      method names to validate
     * @throws IllegalArgumentException if list is null or contains blank entries
     */
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

    /**
     * Trims a string and returns {@code null} when the result is empty.
     *
     * @param value raw value
     * @return trimmed value or {@code null}
     */
    private @Nullable String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Wraps notification executor with metrics instrumentation when enabled and available.
     *
     * @param delegate              delegate notification executor
     * @param properties            bound JSON-RPC properties
     * @param meterRegistryProvider provider for optional meter registry
     * @return instrumented executor when possible, otherwise original delegate
     */
    private JsonRpcNotificationExecutor instrumentNotificationExecutorIfEnabled(
        JsonRpcNotificationExecutor delegate,
        JsonRpcProperties properties,
        ObjectProvider<MeterRegistry> meterRegistryProvider
    ) {
        if (!properties.isMetricsEnabled()) {
            return delegate;
        }
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            return delegate;
        }
        return new InstrumentedJsonRpcNotificationExecutor(
            delegate,
            meterRegistry,
            properties.isMetricsLatencyHistogramEnabled(),
            toPercentileArray(properties.getMetricsLatencyPercentiles())
        );
    }

    /**
     * Validates percentile values used for metrics publication.
     *
     * @param percentiles percentile list
     * @throws IllegalArgumentException if list is null or contains values outside {@code (0,1)}
     */
    private void validatePercentiles(List<Double> percentiles) {
        if (percentiles == null) {
            throw new IllegalArgumentException("jsonrpc.metrics-latency-percentiles must not be null");
        }
        for (Double percentile : percentiles) {
            if (percentile == null || percentile <= 0.0 || percentile >= 1.0) {
                throw new IllegalArgumentException(
                    "jsonrpc.metrics-latency-percentiles values must be greater than 0.0 and less than 1.0");
            }
        }
    }

    /**
     * Converts percentile list into primitive double array with validation.
     *
     * @param percentiles percentile list
     * @return primitive percentile array
     * @throws IllegalArgumentException if a percentile is null or outside {@code (0,1)}
     */
    private double[] toPercentileArray(List<Double> percentiles) {
        if (percentiles == null || percentiles.isEmpty()) {
            return new double[0];
        }
        double[] values = new double[percentiles.size()];
        for (int i = 0; i < percentiles.size(); i++) {
            Double percentile = percentiles.get(i);
            if (percentile == null || percentile <= 0.0 || percentile >= 1.0) {
                throw new IllegalArgumentException(
                    "jsonrpc.metrics-latency-percentiles values must be greater than 0.0 and less than 1.0");
            }
            values[i] = percentile;
        }
        return values;
    }
}
