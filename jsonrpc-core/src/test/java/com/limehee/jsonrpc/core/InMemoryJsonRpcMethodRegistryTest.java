package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryJsonRpcMethodRegistryTest {

    @Test
    void rejectsDuplicateRegistrationByDefault() {
        InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry();

        registry.register("ping", params -> TextNode.valueOf("pong1"));
        assertThrows(IllegalStateException.class,
                () -> registry.register("ping", params -> TextNode.valueOf("pong2")));
    }

    @Test
    void replacesRegistrationWhenConfigured() {
        InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry(
                JsonRpcMethodNamespacePolicy.DISALLOW_RPC_PREFIX,
                JsonRpcMethodRegistrationConflictPolicy.REPLACE
        );

        registry.register("ping", params -> TextNode.valueOf("pong1"));
        registry.register("ping", params -> TextNode.valueOf("pong2"));

        assertEquals("pong2", registry.find("ping").orElseThrow().handle(null).asText());
    }

    @Test
    void alwaysRejectsReservedRpcPrefixEvenWhenAllowAllConfigured() {
        InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry(
                JsonRpcMethodNamespacePolicy.ALLOW_ALL,
                JsonRpcMethodRegistrationConflictPolicy.REPLACE
        );

        assertThrows(IllegalArgumentException.class,
                () -> registry.register("rpc.system", params -> TextNode.valueOf("ok")));
    }
}
