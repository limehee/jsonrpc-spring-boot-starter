package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Functional handler for a single JSON-RPC method.
 */
@FunctionalInterface
public interface JsonRpcMethodHandler {

    /**
     * Handles one JSON-RPC method invocation.
     *
     * @param params request parameters; may be {@code null}
     * @return JSON-RPC result payload
     */
    JsonNode handle(@Nullable JsonNode params);
}
