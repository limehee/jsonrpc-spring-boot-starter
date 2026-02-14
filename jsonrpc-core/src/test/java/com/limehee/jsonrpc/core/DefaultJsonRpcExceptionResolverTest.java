package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultJsonRpcExceptionResolverTest {

    @Test
    void includesErrorDataWhenEnabled() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver(true);

        JsonRpcError error = resolver.resolve(new JsonRpcException(-32000, "domain", TextNode.valueOf("secret")));

        assertEquals(-32000, error.code());
        assertEquals("secret", error.data().asText());
    }

    @Test
    void hidesErrorDataWhenDisabled() {
        DefaultJsonRpcExceptionResolver resolver = new DefaultJsonRpcExceptionResolver(false);

        JsonRpcError error = resolver.resolve(new JsonRpcException(-32000, "domain", TextNode.valueOf("secret")));

        assertEquals(-32000, error.code());
        assertNull(error.data());
    }
}
