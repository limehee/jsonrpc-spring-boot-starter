package com.limehee.jsonrpc.core;

import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultJsonRpcExceptionResolverTest {

    @Test
    void defaultConstructorHidesErrorData() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver();

        JsonRpcError error = resolver.resolve(new JsonRpcException(-32000, "domain", StringNode.valueOf("secret")));

        assertEquals(-32000, error.code());
        assertNull(error.data());
    }

    @Test
    void includesErrorDataWhenEnabled() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver(true);

        JsonRpcError error = resolver.resolve(new JsonRpcException(-32000, "domain", StringNode.valueOf("secret")));

        assertEquals(-32000, error.code());
        assertEquals("secret", error.data().asText());
    }

    @Test
    void hidesErrorDataWhenDisabled() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver(false);

        JsonRpcError error = resolver.resolve(new JsonRpcException(-32000, "domain", StringNode.valueOf("secret")));

        assertEquals(-32000, error.code());
        assertNull(error.data());
    }

    @Test
    void mapsNonJsonRpcExceptionsToInternalError() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver(false);

        JsonRpcError error = resolver.resolve(new IllegalArgumentException("bad"));

        assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, error.code());
        assertEquals(JsonRpcConstants.MESSAGE_INTERNAL_ERROR, error.message());
    }
}
