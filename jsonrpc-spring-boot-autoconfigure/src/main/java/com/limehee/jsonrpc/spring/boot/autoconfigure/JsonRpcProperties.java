package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import com.limehee.jsonrpc.core.JsonRpcParamsTypeViolationCodePolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized Spring Boot configuration properties for JSON-RPC auto-configuration.
 * <p>
 * Properties are bound from the {@code jsonrpc.*} namespace.
 * </p>
 */
@ConfigurationProperties(prefix = "jsonrpc")
public class JsonRpcProperties {

    private boolean enabled = true;
    private String path = "/jsonrpc";
    private int maxBatchSize = 100;
    private int maxRequestBytes = 1_048_576;
    private boolean scanAnnotatedMethods = true;
    private boolean includeErrorData = false;
    private boolean metricsEnabled = true;
    private boolean metricsLatencyHistogramEnabled = false;
    private List<Double> metricsLatencyPercentiles = new ArrayList<>();
    private int metricsMaxMethodTagValues = 100;
    private boolean notificationExecutorEnabled = false;
    private String notificationExecutorBeanName = "";
    private JsonRpcMethodRegistrationConflictPolicy methodRegistrationConflictPolicy = JsonRpcMethodRegistrationConflictPolicy.REJECT;
    private Validation validation = new Validation();
    private List<String> methodAllowlist = new ArrayList<>();
    private List<String> methodDenylist = new ArrayList<>();

    /**
     * Indicates whether the JSON-RPC WebMVC transport endpoint bean is registered.
     *
     * @return {@code true} when endpoint bean registration is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the JSON-RPC WebMVC transport endpoint bean is registered.
     *
     * @param enabled {@code true} to enable endpoint bean registration
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the HTTP path where JSON-RPC requests are served.
     *
     * @return JSON-RPC endpoint path; default is {@code /jsonrpc}
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the HTTP path where JSON-RPC requests are served.
     *
     * @param path endpoint path; must start with {@code /} and contain no whitespace
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the maximum number of entries accepted in a single batch request.
     *
     * @return maximum batch size; default is {@code 100}
     */
    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    /**
     * Sets the maximum number of entries accepted in a single batch request.
     *
     * @param maxBatchSize maximum allowed batch size; must be greater than zero
     */
    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    /**
     * Returns the maximum HTTP payload size accepted by the endpoint.
     *
     * @return maximum request payload size in bytes; default is {@code 1_048_576}
     */
    public int getMaxRequestBytes() {
        return maxRequestBytes;
    }

    /**
     * Sets the maximum HTTP payload size accepted by the endpoint.
     *
     * @param maxRequestBytes maximum request payload size in bytes; must be greater than zero
     */
    public void setMaxRequestBytes(int maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    /**
     * Indicates whether {@link com.limehee.jsonrpc.core.JsonRpcMethod}-annotated methods are scanned and
     * auto-registered.
     *
     * @return {@code true} when annotation scanning is enabled
     */
    public boolean isScanAnnotatedMethods() {
        return scanAnnotatedMethods;
    }

    /**
     * Sets whether annotated methods should be scanned and auto-registered.
     *
     * @param scanAnnotatedMethods {@code true} to enable annotation-driven registration
     */
    public void setScanAnnotatedMethods(boolean scanAnnotatedMethods) {
        this.scanAnnotatedMethods = scanAnnotatedMethods;
    }

    /**
     * Indicates whether mapped exception data should be included in JSON-RPC error payloads.
     *
     * @return {@code true} when error {@code data} field inclusion is enabled
     */
    public boolean isIncludeErrorData() {
        return includeErrorData;
    }

    /**
     * Sets whether mapped exception data should be included in JSON-RPC error payloads.
     *
     * @param includeErrorData {@code true} to include mapped exception data in responses
     */
    public void setIncludeErrorData(boolean includeErrorData) {
        this.includeErrorData = includeErrorData;
    }

    /**
     * Indicates whether Micrometer metrics instrumentation is enabled.
     *
     * @return {@code true} when JSON-RPC metrics are enabled
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    /**
     * Sets whether Micrometer metrics instrumentation is enabled.
     *
     * @param metricsEnabled {@code true} to enable JSON-RPC metrics
     */
    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    /**
     * Indicates whether latency histogram publishing is enabled for supported timers.
     *
     * @return {@code true} when latency histogram publication is enabled
     */
    public boolean isMetricsLatencyHistogramEnabled() {
        return metricsLatencyHistogramEnabled;
    }

    /**
     * Sets whether latency histogram publishing is enabled for supported timers.
     *
     * @param metricsLatencyHistogramEnabled {@code true} to enable latency histograms
     */
    public void setMetricsLatencyHistogramEnabled(boolean metricsLatencyHistogramEnabled) {
        this.metricsLatencyHistogramEnabled = metricsLatencyHistogramEnabled;
    }

    /**
     * Returns configured latency percentiles to publish for supported metrics.
     *
     * @return latency percentile list in {@code (0,1)} range
     */
    public List<Double> getMetricsLatencyPercentiles() {
        return metricsLatencyPercentiles;
    }

    /**
     * Sets configured latency percentiles to publish for supported metrics.
     *
     * @param metricsLatencyPercentiles percentile list; each value must be greater than {@code 0.0} and less than
     *                                  {@code 1.0}
     */
    public void setMetricsLatencyPercentiles(List<Double> metricsLatencyPercentiles) {
        this.metricsLatencyPercentiles = metricsLatencyPercentiles;
    }

    /**
     * Returns the maximum number of distinct method tag values retained for metrics.
     *
     * @return method tag cardinality cap; default is {@code 100}
     */
    public int getMetricsMaxMethodTagValues() {
        return metricsMaxMethodTagValues;
    }

    /**
     * Sets the maximum number of distinct method tag values retained for metrics.
     *
     * @param metricsMaxMethodTagValues method tag cardinality cap; must be greater than zero
     */
    public void setMetricsMaxMethodTagValues(int metricsMaxMethodTagValues) {
        this.metricsMaxMethodTagValues = metricsMaxMethodTagValues;
    }

    /**
     * Indicates whether notification handling should prefer an executor-backed path.
     * <p>
     * When enabled, auto-configuration attempts to choose a Spring {@link java.util.concurrent.Executor}. If no
     * suitable executor is resolved, handling falls back to direct execution.
     * </p>
     *
     * @return {@code true} when executor-backed notification handling is enabled
     */
    public boolean isNotificationExecutorEnabled() {
        return notificationExecutorEnabled;
    }

    /**
     * Sets whether notification handling should prefer an executor-backed path.
     *
     * @param notificationExecutorEnabled {@code true} to enable executor-backed handling
     */
    public void setNotificationExecutorEnabled(boolean notificationExecutorEnabled) {
        this.notificationExecutorEnabled = notificationExecutorEnabled;
    }

    /**
     * Returns the preferred Spring {@link java.util.concurrent.Executor} bean name for notification execution.
     *
     * @return executor bean name, or empty when auto-selection should be used
     */
    public String getNotificationExecutorBeanName() {
        return notificationExecutorBeanName;
    }

    /**
     * Sets the preferred Spring {@link java.util.concurrent.Executor} bean name for notification execution.
     *
     * @param notificationExecutorBeanName preferred executor bean name, or empty for auto-selection
     */
    public void setNotificationExecutorBeanName(String notificationExecutorBeanName) {
        this.notificationExecutorBeanName = notificationExecutorBeanName;
    }

    /**
     * Returns the duplicate method registration conflict policy.
     *
     * @return conflict policy used by method registry
     */
    public JsonRpcMethodRegistrationConflictPolicy getMethodRegistrationConflictPolicy() {
        return methodRegistrationConflictPolicy;
    }

    /**
     * Sets the duplicate method registration conflict policy.
     *
     * @param methodRegistrationConflictPolicy conflict policy used by method registry
     */
    public void setMethodRegistrationConflictPolicy(
        JsonRpcMethodRegistrationConflictPolicy methodRegistrationConflictPolicy
    ) {
        this.methodRegistrationConflictPolicy = methodRegistrationConflictPolicy;
    }

    /**
     * Returns validation-related options.
     *
     * @return nested validation options
     */
    public Validation getValidation() {
        return validation;
    }

    /**
     * Sets validation-related options.
     *
     * @param validation nested validation options; must not be {@code null}
     */
    public void setValidation(Validation validation) {
        this.validation = Objects.requireNonNull(validation, "validation");
    }

    /**
     * Returns method allowlist used by access control interceptor.
     *
     * @return configured allowlist entries
     */
    public List<String> getMethodAllowlist() {
        return methodAllowlist;
    }

    /**
     * Sets method allowlist used by access control interceptor.
     *
     * @param methodAllowlist allowed method names; empty list disables allowlist filtering
     */
    public void setMethodAllowlist(List<String> methodAllowlist) {
        this.methodAllowlist = methodAllowlist;
    }

    /**
     * Returns method denylist used by access control interceptor.
     *
     * @return configured denylist entries
     */
    public List<String> getMethodDenylist() {
        return methodDenylist;
    }

    /**
     * Sets method denylist used by access control interceptor.
     *
     * @param methodDenylist denied method names
     */
    public void setMethodDenylist(List<String> methodDenylist) {
        this.methodDenylist = methodDenylist;
    }

    /**
     * Nested validation configuration under {@code jsonrpc.validation.*}.
     */
    public static final class Validation {

        private Request request = new Request();
        private Response response = new Response();

        /**
         * Returns request-validation options.
         *
         * @return request-validation options
         */
        public Request getRequest() {
            return request;
        }

        /**
         * Sets request-validation options.
         *
         * @param request request-validation options; must not be {@code null}
         */
        public void setRequest(Request request) {
            this.request = Objects.requireNonNull(request, "request");
        }

        /**
         * Returns response-validation options.
         *
         * @return response-validation options
         */
        public Response getResponse() {
            return response;
        }

        /**
         * Sets response-validation options.
         *
         * @param response response-validation options; must not be {@code null}
         */
        public void setResponse(Response response) {
            this.response = Objects.requireNonNull(response, "response");
        }

        /**
         * Request-side validation options under {@code jsonrpc.validation.request.*}.
         */
        public static final class Request {

            private JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy =
                JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS;

            /**
             * Returns the error-code mapping policy used when request {@code params} exists but is neither an object
             * nor an array.
             *
             * @return params-type violation error-code policy
             */
            public JsonRpcParamsTypeViolationCodePolicy getParamsTypeViolationCodePolicy() {
                return paramsTypeViolationCodePolicy;
            }

            /**
             * Sets the error-code mapping policy used when request {@code params} exists but is neither an object nor
             * an array.
             *
             * @param paramsTypeViolationCodePolicy params-type violation error-code policy
             */
            public void setParamsTypeViolationCodePolicy(
                JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy
            ) {
                this.paramsTypeViolationCodePolicy = Objects.requireNonNull(
                    paramsTypeViolationCodePolicy,
                    "paramsTypeViolationCodePolicy"
                );
            }
        }

        /**
         * Response-side validation options under {@code jsonrpc.validation.response.*}.
         */
        public static final class Response {

            private boolean requireJsonRpcVersion20 = true;
            private boolean requireResponseIdMember = true;
            private boolean allowNullResponseId = true;
            private boolean allowStringResponseId = true;
            private boolean allowNumericResponseId = true;
            private boolean allowFractionalResponseId = true;
            private boolean requireExclusiveResultOrError = true;
            private boolean requireErrorObjectWhenPresent = true;
            private boolean requireIntegerErrorCode = true;
            private boolean requireStringErrorMessage = true;
            private boolean allowRequestFieldsInResponse = true;

            /**
             * Indicates whether {@code jsonrpc == "2.0"} is required on incoming responses.
             *
             * @return {@code true} when version enforcement is enabled
             */
            public boolean isRequireJsonRpcVersion20() {
                return requireJsonRpcVersion20;
            }

            /**
             * Sets whether {@code jsonrpc == "2.0"} is required on incoming responses.
             *
             * @param requireJsonRpcVersion20 {@code true} to enforce version field
             */
            public void setRequireJsonRpcVersion20(boolean requireJsonRpcVersion20) {
                this.requireJsonRpcVersion20 = requireJsonRpcVersion20;
            }

            /**
             * Indicates whether incoming responses must include an {@code id} member.
             *
             * @return {@code true} when response {@code id} member is required
             */
            public boolean isRequireResponseIdMember() {
                return requireResponseIdMember;
            }

            /**
             * Sets whether incoming responses must include an {@code id} member.
             *
             * @param requireResponseIdMember {@code true} to require response {@code id}
             */
            public void setRequireResponseIdMember(boolean requireResponseIdMember) {
                this.requireResponseIdMember = requireResponseIdMember;
            }

            /**
             * Indicates whether {@code id: null} is allowed in incoming responses.
             *
             * @return {@code true} when null IDs are accepted
             */
            public boolean isAllowNullResponseId() {
                return allowNullResponseId;
            }

            /**
             * Sets whether {@code id: null} is allowed in incoming responses.
             *
             * @param allowNullResponseId {@code true} to accept null IDs
             */
            public void setAllowNullResponseId(boolean allowNullResponseId) {
                this.allowNullResponseId = allowNullResponseId;
            }

            /**
             * Indicates whether textual response IDs are allowed.
             *
             * @return {@code true} when string IDs are accepted
             */
            public boolean isAllowStringResponseId() {
                return allowStringResponseId;
            }

            /**
             * Sets whether textual response IDs are allowed.
             *
             * @param allowStringResponseId {@code true} to accept string IDs
             */
            public void setAllowStringResponseId(boolean allowStringResponseId) {
                this.allowStringResponseId = allowStringResponseId;
            }

            /**
             * Indicates whether numeric response IDs are allowed.
             *
             * @return {@code true} when numeric IDs are accepted
             */
            public boolean isAllowNumericResponseId() {
                return allowNumericResponseId;
            }

            /**
             * Sets whether numeric response IDs are allowed.
             *
             * @param allowNumericResponseId {@code true} to accept numeric IDs
             */
            public void setAllowNumericResponseId(boolean allowNumericResponseId) {
                this.allowNumericResponseId = allowNumericResponseId;
            }

            /**
             * Indicates whether fractional numeric response IDs are allowed.
             *
             * @return {@code true} when fractional numeric IDs are accepted
             */
            public boolean isAllowFractionalResponseId() {
                return allowFractionalResponseId;
            }

            /**
             * Sets whether fractional numeric response IDs are allowed.
             *
             * @param allowFractionalResponseId {@code true} to accept fractional numeric IDs
             */
            public void setAllowFractionalResponseId(boolean allowFractionalResponseId) {
                this.allowFractionalResponseId = allowFractionalResponseId;
            }

            /**
             * Indicates whether exactly one of {@code result}/{@code error} must exist.
             *
             * @return {@code true} when exclusive result/error enforcement is enabled
             */
            public boolean isRequireExclusiveResultOrError() {
                return requireExclusiveResultOrError;
            }

            /**
             * Sets whether exactly one of {@code result}/{@code error} must exist.
             *
             * @param requireExclusiveResultOrError {@code true} to enforce exclusivity
             */
            public void setRequireExclusiveResultOrError(boolean requireExclusiveResultOrError) {
                this.requireExclusiveResultOrError = requireExclusiveResultOrError;
            }

            /**
             * Indicates whether {@code error} must be an object when present.
             *
             * @return {@code true} when error object enforcement is enabled
             */
            public boolean isRequireErrorObjectWhenPresent() {
                return requireErrorObjectWhenPresent;
            }

            /**
             * Sets whether {@code error} must be an object when present.
             *
             * @param requireErrorObjectWhenPresent {@code true} to enforce error object shape
             */
            public void setRequireErrorObjectWhenPresent(boolean requireErrorObjectWhenPresent) {
                this.requireErrorObjectWhenPresent = requireErrorObjectWhenPresent;
            }

            /**
             * Indicates whether {@code error.code} must be an integer number.
             *
             * @return {@code true} when integer error-code enforcement is enabled
             */
            public boolean isRequireIntegerErrorCode() {
                return requireIntegerErrorCode;
            }

            /**
             * Sets whether {@code error.code} must be an integer number.
             *
             * @param requireIntegerErrorCode {@code true} to enforce integer error code
             */
            public void setRequireIntegerErrorCode(boolean requireIntegerErrorCode) {
                this.requireIntegerErrorCode = requireIntegerErrorCode;
            }

            /**
             * Indicates whether {@code error.message} must be a string.
             *
             * @return {@code true} when string error-message enforcement is enabled
             */
            public boolean isRequireStringErrorMessage() {
                return requireStringErrorMessage;
            }

            /**
             * Sets whether {@code error.message} must be a string.
             *
             * @param requireStringErrorMessage {@code true} to enforce string error message
             */
            public void setRequireStringErrorMessage(boolean requireStringErrorMessage) {
                this.requireStringErrorMessage = requireStringErrorMessage;
            }

            /**
             * Indicates whether request-only fields like {@code method}/{@code params} are allowed in response
             * objects.
             *
             * @return {@code true} when request fields are tolerated in responses
             */
            public boolean isAllowRequestFieldsInResponse() {
                return allowRequestFieldsInResponse;
            }

            /**
             * Sets whether request-only fields like {@code method}/{@code params} are allowed in response objects.
             *
             * @param allowRequestFieldsInResponse {@code true} to allow request fields in response
             */
            public void setAllowRequestFieldsInResponse(boolean allowRequestFieldsInResponse) {
                this.allowRequestFieldsInResponse = allowRequestFieldsInResponse;
            }
        }
    }
}
