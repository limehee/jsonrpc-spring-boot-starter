package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Default invoker delegating directly to the handler.
 */
public class DefaultJsonRpcMethodInvoker implements JsonRpcMethodInvoker {

    /**
     * Invokes a handler with provided params.
     *
     * @param handler method handler
     * @param params optional request params
     * @return handler result
     */
    @Override
    public JsonNode invoke(JsonRpcMethodHandler handler, @Nullable JsonNode params) {
        return handler.handle(params);
    }
}
