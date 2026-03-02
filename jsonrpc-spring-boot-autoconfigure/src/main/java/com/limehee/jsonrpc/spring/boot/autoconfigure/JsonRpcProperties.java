package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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
     * Indicates whether {@link com.limehee.jsonrpc.core.JsonRpcMethod}-annotated methods are
     * scanned and auto-registered.
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
     * @param metricsLatencyPercentiles percentile list; each value must be greater than
     *                                  {@code 0.0} and less than {@code 1.0}
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
     * When enabled, auto-configuration attempts to choose a Spring {@link java.util.concurrent.Executor}.
     * If no suitable executor is resolved, handling falls back to direct execution.
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
     * Returns the preferred Spring {@link java.util.concurrent.Executor} bean name for
     * notification execution.
     *
     * @return executor bean name, or empty when auto-selection should be used
     */
    public String getNotificationExecutorBeanName() {
        return notificationExecutorBeanName;
    }

    /**
     * Sets the preferred Spring {@link java.util.concurrent.Executor} bean name for
     * notification execution.
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
}
