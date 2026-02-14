package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsonRpcDispatcherTest {

    @Test
    void dispatchReturnsSuccess() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));

        JsonRpcRequest request = new JsonRpcRequest();
        request.setJsonrpc("2.0");
        request.setMethod("ping");
        request.setId(IntNode.valueOf(1));

        JsonRpcResponse response = dispatcher.dispatch(request);

        assertNull(response.getError());
        assertNotNull(response.getResult());
        assertEquals("pong", response.getResult().asText());
    }

    @Test
    void dispatchReturnsMethodNotFound() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcRequest request = new JsonRpcRequest();
        request.setJsonrpc("2.0");
        request.setMethod("unknown");
        request.setId(IntNode.valueOf(1));

        JsonRpcResponse response = dispatcher.dispatch(request);

        assertNotNull(response.getError());
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, response.getError().getCode());
    }
}
