package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Parsed JSON-RPC request model used by the dispatcher pipeline.
 * <p>
 * This type is primarily produced by {@link DefaultJsonRpcRequestParser} and consumed internally by validation,
 * interception, and dispatch components. For outbound JSON-RPC request composition, prefer
 * {@link JsonRpcRequestBuilder}.
 * </p>
 *
 * @param jsonrpc   protocol version string from payload; may be {@code null}
 * @param id        request id value; may be {@code null}
 * @param method    method name; may be {@code null}
 * @param params    raw params payload; may be {@code null}
 * @param idPresent whether the original payload explicitly contained an {@code id} field
 * @param source    original request object node; may be {@code null}
 */
public record JsonRpcRequest(
    @Nullable String jsonrpc,
    @Nullable JsonNode id,
    @Nullable String method,
    @Nullable JsonNode params,
    boolean idPresent,
    @Nullable JsonNode source
) {

    /**
     * Creates a request without storing the original source node.
     *
     * @param jsonrpc   protocol version string from payload; may be {@code null}
     * @param id        request id value; may be {@code null}
     * @param method    method name; may be {@code null}
     * @param params    raw params payload; may be {@code null}
     * @param idPresent whether the original payload explicitly contained an {@code id} field
     */
    public JsonRpcRequest(
        @Nullable String jsonrpc,
        @Nullable JsonNode id,
        @Nullable String method,
        @Nullable JsonNode params,
        boolean idPresent
    ) {
        this(jsonrpc, id, method, params, idPresent, null);
    }

    /**
     * Determines whether this request is a notification.
     *
     * @return {@code true} when no explicit {@code id} was provided
     */
    public boolean isNotification() {
        return !idPresent;
    }
}
