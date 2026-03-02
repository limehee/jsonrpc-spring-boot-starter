package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Default response composer delegating to {@link JsonRpcResponse} factories.
 */
public class DefaultJsonRpcResponseComposer implements JsonRpcResponseComposer {

    /**
     * Creates a success response.
     *
     * @param id request id
     * @param result success payload
     * @return success response
     */
    @Override
    public JsonRpcResponse success(@Nullable JsonNode id, JsonNode result) {
        return JsonRpcResponse.success(id, result);
    }

    /**
     * Creates an error response.
     *
     * @param id request id
     * @param error error payload
     * @return error response
     */
    @Override
    public JsonRpcResponse error(@Nullable JsonNode id, JsonRpcError error) {
        return JsonRpcResponse.error(id, error);
    }
}
