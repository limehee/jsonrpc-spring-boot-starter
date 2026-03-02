package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Intercepts dispatcher lifecycle events.
 * <p>
 * All callbacks are optional and have no-op defaults.
 */
public interface JsonRpcInterceptor {

    /**
     * Called before parsing/validation for each single request node.
     *
     * @param rawRequest raw JSON request node
     */
    default void beforeValidate(JsonNode rawRequest) {
    }

    /**
     * Called right before a method handler is invoked.
     *
     * @param request validated request model
     */
    default void beforeInvoke(JsonRpcRequest request) {
    }

    /**
     * Called right after a method handler returns successfully.
     *
     * @param request validated request model
     * @param result result payload returned by the handler
     */
    default void afterInvoke(JsonRpcRequest request, JsonNode result) {
    }

    /**
     * Called when any error is mapped to a JSON-RPC error.
     *
     * @param request request model when available; {@code null} when parsing failed before request construction
     * @param throwable original exception
     * @param mappedError mapped JSON-RPC error object
     */
    default void onError(@Nullable JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
    }
}
