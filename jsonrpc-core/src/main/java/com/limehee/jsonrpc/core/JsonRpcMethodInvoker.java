package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Invokes a registered JSON-RPC method handler.
 */
public interface JsonRpcMethodInvoker {

    /**
     * Invokes a handler with optional parameters.
     *
     * @param handler handler to invoke
     * @param params JSON-RPC params node; may be {@code null}
     * @return JSON node returned by the handler
     * @throws Exception when handler invocation fails
     */
    JsonNode invoke(JsonRpcMethodHandler handler, @Nullable JsonNode params) throws Exception;
}
