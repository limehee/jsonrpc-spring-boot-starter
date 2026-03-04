package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

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
        assertEquals("secret", error.data().asString());
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

    @Test
    void fallsBackToInternalErrorMessageWhenDomainExceptionMessageIsNull() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver(false);

        JsonRpcError error = resolver.resolve(new JsonRpcException(-32000, null));

        assertEquals(-32000, error.code());
        assertEquals(JsonRpcConstants.MESSAGE_INTERNAL_ERROR, error.message());
    }
}
