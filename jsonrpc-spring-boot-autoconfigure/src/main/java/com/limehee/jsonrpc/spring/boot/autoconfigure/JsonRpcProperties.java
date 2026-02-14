package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "jsonrpc")
public class JsonRpcProperties {

    private boolean enabled = true;
    private String path = "/jsonrpc";
    private int maxBatchSize = 100;
    private int maxRequestBytes = 1_048_576;
    private boolean scanAnnotatedMethods = true;
    private boolean includeErrorData = false;
    private boolean metricsEnabled = true;
    private boolean notificationExecutorEnabled = false;
    private String notificationExecutorBeanName = "";
    private JsonRpcMethodRegistrationConflictPolicy methodRegistrationConflictPolicy = JsonRpcMethodRegistrationConflictPolicy.REJECT;
    private List<String> methodAllowlist = new ArrayList<>();
    private List<String> methodDenylist = new ArrayList<>();

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

    public String getNotificationExecutorBeanName() {
        return notificationExecutorBeanName;
    }

    public void setNotificationExecutorBeanName(String notificationExecutorBeanName) {
        this.notificationExecutorBeanName = notificationExecutorBeanName;
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
