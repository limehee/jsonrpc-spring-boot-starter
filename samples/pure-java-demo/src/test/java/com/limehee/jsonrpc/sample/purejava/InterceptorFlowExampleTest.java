package com.limehee.jsonrpc.sample.purejava;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorFlowExampleTest {

    @Test
    void recordsExpectedInterceptorOrderForSuccess() throws Exception {
        InterceptorFlowExample.Result result = InterceptorFlowExample.execute("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);

        assertEquals(3, result.events().size());
        assertEquals("beforeValidate", result.events().get(0));
        assertEquals("beforeInvoke:ping", result.events().get(1));
        assertEquals("afterInvoke:pong", result.events().get(2));
        assertTrue(result.dispatchResult().hasResponse());
    }

    @Test
    void recordsOnErrorAndKeepsResponseWhenHandlerFails() throws Exception {
        InterceptorFlowExample.Result result = InterceptorFlowExample.execute("""
                {"jsonrpc":"2.0","method":"explode","id":2}
                """);

        assertTrue(result.events().contains("onError:-32603"));
        assertEquals(-32603, result.dispatchResult().singleResponse().orElseThrow().error().code());
    }

    @Test
    void recordsOnErrorForMethodResolutionFailure() throws Exception {
        InterceptorFlowExample.Result result = InterceptorFlowExample.execute("""
                {"jsonrpc":"2.0","method":"missing","id":3}
                """);

        assertTrue(result.events().contains("onError:-32601"));
        assertEquals(-32601, result.dispatchResult().singleResponse().orElseThrow().error().code());
    }
}
