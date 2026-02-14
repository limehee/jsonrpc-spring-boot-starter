package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcMethodNamespacePolicy;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "jsonrpc")
public class JsonRpcProperties {

    private boolean enabled = true;
    @NotBlank(message = "jsonrpc.path must not be blank")
    @Pattern(regexp = "^/\\S*$", message = "jsonrpc.path must start with '/' and must not contain whitespace")
    private String path = "/jsonrpc";
    @Positive(message = "jsonrpc.max-batch-size must be greater than 0")
    private int maxBatchSize = 100;
    @Positive(message = "jsonrpc.max-request-bytes must be greater than 0")
    private int maxRequestBytes = 1_048_576;
    @NotNull
    private JsonRpcMethodNamespacePolicy methodNamespacePolicy = JsonRpcMethodNamespacePolicy.DISALLOW_RPC_PREFIX;
    private boolean scanAnnotatedMethods = true;
    private boolean includeErrorData = false;
    private boolean metricsEnabled = true;
    private boolean notificationExecutorEnabled = false;
    @NotNull
    private JsonRpcMethodRegistrationConflictPolicy methodRegistrationConflictPolicy = JsonRpcMethodRegistrationConflictPolicy.REJECT;
    @NotNull
    private List<@NotBlank(message = "jsonrpc.method-allowlist entries must not be blank") String> methodAllowlist = new ArrayList<>();
    @NotNull
    private List<@NotBlank(message = "jsonrpc.method-denylist entries must not be blank") String> methodDenylist = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public int getMaxRequestBytes() {
        return maxRequestBytes;
    }

    public void setMaxRequestBytes(int maxRequestBytes) {
        this.maxRequestBytes = maxRequestBytes;
    }

    public JsonRpcMethodNamespacePolicy getMethodNamespacePolicy() {
        return methodNamespacePolicy;
    }

    public void setMethodNamespacePolicy(JsonRpcMethodNamespacePolicy methodNamespacePolicy) {
        this.methodNamespacePolicy = methodNamespacePolicy;
    }

    public boolean isScanAnnotatedMethods() {
        return scanAnnotatedMethods;
    }

    public void setScanAnnotatedMethods(boolean scanAnnotatedMethods) {
        this.scanAnnotatedMethods = scanAnnotatedMethods;
    }

    public boolean isIncludeErrorData() {
        return includeErrorData;
    }

    public void setIncludeErrorData(boolean includeErrorData) {
        this.includeErrorData = includeErrorData;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public boolean isNotificationExecutorEnabled() {
        return notificationExecutorEnabled;
    }

    public void setNotificationExecutorEnabled(boolean notificationExecutorEnabled) {
        this.notificationExecutorEnabled = notificationExecutorEnabled;
    }

    public JsonRpcMethodRegistrationConflictPolicy getMethodRegistrationConflictPolicy() {
        return methodRegistrationConflictPolicy;
    }

    public void setMethodRegistrationConflictPolicy(
            JsonRpcMethodRegistrationConflictPolicy methodRegistrationConflictPolicy
    ) {
        this.methodRegistrationConflictPolicy = methodRegistrationConflictPolicy;
    }

    public List<String> getMethodAllowlist() {
        return methodAllowlist;
    }

    public void setMethodAllowlist(List<String> methodAllowlist) {
        this.methodAllowlist = methodAllowlist;
    }

    public List<String> getMethodDenylist() {
        return methodDenylist;
    }

    public void setMethodDenylist(List<String> methodDenylist) {
        this.methodDenylist = methodDenylist;
    }
}
