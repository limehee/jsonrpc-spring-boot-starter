package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.IntNode;

class JsonRpcErrorTest {

    @Test
    void ofWithDataCreatesErrorWithProvidedData() {
        IntNode data = IntNode.valueOf(99);

        JsonRpcError error = JsonRpcError.of(JsonRpcErrorCode.INTERNAL_ERROR, "boom", data);

        assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, error.code());
        assertEquals("boom", error.message());
        assertSame(data, error.data());
    }
}
