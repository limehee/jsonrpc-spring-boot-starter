package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.StringNode;

class InMemoryJsonRpcMethodRegistryTest {

    @Test
    void rejectsDuplicateRegistrationByDefault() {
        InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry();

        registry.register("ping", params -> StringNode.valueOf("pong1"));
        assertThrows(IllegalStateException.class,
            () -> registry.register("ping", params -> StringNode.valueOf("pong2")));
    }

    @Test
    void replacesRegistrationWhenConfigured() {
        InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry(
            JsonRpcMethodRegistrationConflictPolicy.REPLACE
        );

        registry.register("ping", params -> StringNode.valueOf("pong1"));
        registry.register("ping", params -> StringNode.valueOf("pong2"));

        assertEquals("pong2", registry.find("ping").orElseThrow().handle(null).asString());
    }

    @Test
    void alwaysRejectsReservedRpcPrefix() {
        InMemoryJsonRpcMethodRegistry registry = new InMemoryJsonRpcMethodRegistry(
            JsonRpcMethodRegistrationConflictPolicy.REPLACE
        );

        assertThrows(IllegalArgumentException.class,
            () -> registry.register("rpc.system", params -> StringNode.valueOf("ok")));
    }

    @Test
    void rejectsNullConflictPolicy() {
        assertThrows(NullPointerException.class, () -> new InMemoryJsonRpcMethodRegistry(null));
    }
}
