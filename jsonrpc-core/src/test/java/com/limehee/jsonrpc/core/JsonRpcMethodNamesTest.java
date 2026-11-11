package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

class JsonRpcMethodNamesTest {

    private static final List<String> REJECTED = List.of("", " ", "\t\n", "rpc.", "rpc.system", "rpc.discover");
    private static final List<String> ACCEPTED = List.of("ping", "user.find", "RPC.upper", "rpc", "rpcx.call",
        " rpc.leading-space");

    @Test
    void classifiesBlankAndReservedNames() {
        assertTrue(JsonRpcMethodNames.isBlank(""));
        assertTrue(JsonRpcMethodNames.isBlank(" \t"));
        assertFalse(JsonRpcMethodNames.isBlank("ping"));

        assertTrue(JsonRpcMethodNames.isReserved("rpc."));
        assertTrue(JsonRpcMethodNames.isReserved("rpc.system"));
        assertFalse(JsonRpcMethodNames.isReserved("rpc"));
        assertFalse(JsonRpcMethodNames.isReserved("RPC.system"));

        REJECTED.forEach(name -> assertTrue(JsonRpcMethodNames.isRejected(name), name));
        ACCEPTED.forEach(name -> assertFalse(JsonRpcMethodNames.isRejected(name), name));
    }

    @Test
    void registryEnforcesSameRules() {
        for (String name : REJECTED) {
            InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry();
            assertThrows(IllegalArgumentException.class,
                () -> registry.register(name, params -> StringNode.valueOf("ok")), name);
        }
        for (String name : ACCEPTED) {
            InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry();
            assertDoesNotThrow(() -> registry.register(name, params -> StringNode.valueOf("ok")), name);
        }
    }

    @Test
    void requestBuilderEnforcesSameRules() {
        for (String name : REJECTED) {
            assertThrows(IllegalArgumentException.class, () -> JsonRpcRequestBuilder.request(name), name);
            assertThrows(IllegalArgumentException.class, () -> JsonRpcRequestBuilder.notification(name), name);
        }
        for (String name : ACCEPTED) {
            assertDoesNotThrow(() -> JsonRpcRequestBuilder.request(name), name);
            assertDoesNotThrow(() -> JsonRpcRequestBuilder.notification(name), name);
        }
    }

    @Test
    void requestValidatorEnforcesSameRules() {
        DefaultJsonRpcRequestValidator validator = new DefaultJsonRpcRequestValidator();
        for (String name : REJECTED) {
            JsonRpcException ex = assertThrows(JsonRpcException.class,
                () -> validator.validate(request(name)), name);
            assertEquals(JsonRpcErrorCode.INVALID_REQUEST, ex.getCode(), name);
        }
        for (String name : ACCEPTED) {
            assertDoesNotThrow(() -> validator.validate(request(name)), name);
        }
    }

    @Test
    void dispatcherEnforcesSameRules() {
        for (String name : REJECTED) {
            JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
            JsonRpcResponse response = dispatcher.dispatch(requestNode(name)).singleResponse().orElseThrow();
            assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code(), name);
        }
        for (String name : ACCEPTED) {
            JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
            dispatcher.register(name, params -> StringNode.valueOf("ok"));
            JsonRpcResponse response = dispatcher.dispatch(requestNode(name)).singleResponse().orElseThrow();
            assertEquals("ok", response.result().asString(), name);
        }
    }

    private static JsonRpcRequest request(String method) {
        return new DefaultJsonRpcRequestParser().parse(requestNode(method));
    }

    private static ObjectNode requestNode(String method) {
        ObjectNode node = JsonMapper.builder().build().createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("method", method);
        node.put("id", 1);
        return node;
    }
}
