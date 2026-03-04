package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * JSON-RPC error object model.
 *
 * @param code    JSON-RPC error code
 * @param message human-readable error message
 * @param data    optional error data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcError(int code, String message, @Nullable JsonNode data) {

    /**
     * Creates an error object without optional data payload.
     *
     * @param code    JSON-RPC error code
     * @param message human-readable error message
     * @return error object
     */
    public static JsonRpcError of(int code, String message) {
        return new JsonRpcError(code, message, null);
    }
}
