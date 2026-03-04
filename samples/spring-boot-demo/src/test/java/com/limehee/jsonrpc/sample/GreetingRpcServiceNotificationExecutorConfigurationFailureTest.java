package com.limehee.jsonrpc.sample;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class GreetingRpcServiceNotificationExecutorConfigurationFailureTest {

    @Test
    void failsStartupWhenConfiguredNotificationExecutorBeanIsMissing() {
        try {
            new SpringApplicationBuilder(DemoApplication.class)
                .properties(
                    "spring.main.web-application-type=none",
                    "jsonrpc.notification-executor-enabled=true",
                    "jsonrpc.notification-executor-bean-name=missingExecutor"
                )
                .run()
                .close();
            fail("Expected startup failure");
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            assertTrue(root.getMessage().contains(
                "jsonrpc.notification-executor-bean-name points to missing Executor bean: missingExecutor"));
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
