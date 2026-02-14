package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.node.IntNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.spring.boot.autoconfigure.support.JsonRpcMethodAccessInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonRpcMethodAccessInterceptorTest {

    @Test
    void allowsMethodWhenNoListsConfigured() {
        JsonRpcMethodAccessInterceptor interceptor = new JsonRpcMethodAccessInterceptor(Set.of(), Set.of());

        assertDoesNotThrow(() -> interceptor.beforeInvoke(request("ping")));
    }

    @Test
    void blocksMethodWhenNotIncludedInAllowlist() {
        JsonRpcMethodAccessInterceptor interceptor = new JsonRpcMethodAccessInterceptor(Set.of("ping"), Set.of());

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> interceptor.beforeInvoke(request("pong")));
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, ex.getCode());
    }

    @Test
    void blocksMethodWhenIncludedInDenylist() {
        JsonRpcMethodAccessInterceptor interceptor = new JsonRpcMethodAccessInterceptor(Set.of(), Set.of("ping"));

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> interceptor.beforeInvoke(request("ping")));
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, ex.getCode());
    }

    @Test
    void denylistStillBlocksWhenMethodIsAlsoInAllowlist() {
        JsonRpcMethodAccessInterceptor interceptor = new JsonRpcMethodAccessInterceptor(Set.of("ping"), Set.of("ping"));

        JsonRpcException ex = assertThrows(JsonRpcException.class, () -> interceptor.beforeInvoke(request("ping")));
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, ex.getCode());
    }

    @Test
    void hasHighestPrecedenceOrder() {
        JsonRpcMethodAccessInterceptor interceptor = new JsonRpcMethodAccessInterceptor(Set.of(), Set.of());

        assertEquals(Ordered.HIGHEST_PRECEDENCE, interceptor.getOrder());
    }

    private JsonRpcRequest request(String method) {
        return new JsonRpcRequest("2.0", IntNode.valueOf(1), method, null, true);
    }
}
