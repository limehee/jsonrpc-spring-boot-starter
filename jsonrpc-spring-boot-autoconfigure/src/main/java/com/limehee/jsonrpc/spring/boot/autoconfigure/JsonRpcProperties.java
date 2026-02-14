package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcMethodNamespacePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jsonrpc")
public class JsonRpcProperties {

    private boolean enabled = true;
    private String path = "/jsonrpc";
    private int maxBatchSize = 100;
    private int maxRequestBytes = 1_048_576;
    private JsonRpcMethodNamespacePolicy methodNamespacePolicy = JsonRpcMethodNamespacePolicy.DISALLOW_RPC_PREFIX;
    private boolean scanAnnotatedMethods = true;
    private boolean includeErrorData = false;
    private boolean metricsEnabled = true;

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
}
