package com.limehee.jsonrpc.sample;

import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jsonrpc.notification-executor-enabled=true",
        "jsonrpc.notification-executor-bean-name=sampleNotificationExecutor"
})
@Import(GreetingRpcServiceNotificationExecutorIntegrationTest.NotificationExecutorTestConfig.class)
class GreetingRpcServiceNotificationExecutorIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Autowired
    private NotificationProbe probe;

    @Test
    void usesConfiguredExecutorForNotificationRequests() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"notify.mark"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(probe.latch.await(1, TimeUnit.SECONDS));
        assertEquals(1, probe.executorDispatchCount.get());
        assertEquals(1, probe.handlerInvocationCount.get());
    }

    static final class NotificationProbe {
        private final AtomicInteger executorDispatchCount = new AtomicInteger();
        private final AtomicInteger handlerInvocationCount = new AtomicInteger();
        private final CountDownLatch latch = new CountDownLatch(1);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class NotificationExecutorTestConfig {
        @Bean("sampleNotificationExecutor")
        Executor sampleNotificationExecutor(NotificationProbe probe) {
            return command -> {
                probe.executorDispatchCount.incrementAndGet();
                command.run();
            };
        }

        @Bean
        NotificationProbe notificationProbe() {
            return new NotificationProbe();
        }

        @Bean
        JsonRpcMethodRegistration notificationMethod(NotificationProbe probe) {
            return JsonRpcMethodRegistration.of("notify.mark", params -> {
                probe.handlerInvocationCount.incrementAndGet();
                probe.latch.countDown();
                return StringNode.valueOf("ok");
            });
        }
    }
}
