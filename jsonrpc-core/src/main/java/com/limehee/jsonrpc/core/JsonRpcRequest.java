package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Parsed JSON-RPC request model used by the dispatcher pipeline.
 *
 * @param jsonrpc   protocol version string from payload; may be {@code null}
 * @param id        request id value; may be {@code null}
 * @param method    method name; may be {@code null}
 * @param params    raw params payload; may be {@code null}
 * @param idPresent whether the original payload explicitly contained an {@code id} field
 */
public record JsonRpcRequest(
    @Nullable String jsonrpc,
    @Nullable JsonNode id,
    @Nullable String method,
    @Nullable JsonNode params,
    boolean idPresent
) {

    /**
     * Determines whether this request is a notification.
     *
     * @return {@code true} when no explicit {@code id} was provided
     */
    public boolean isNotification() {
        return !idPresent;
    }
}
