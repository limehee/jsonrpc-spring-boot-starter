package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Creates JSON-RPC response payloads from method invocation outcomes.
 */
public interface JsonRpcResponseComposer {

    /**
     * Creates a successful JSON-RPC response.
     *
     * @param id     request identifier; may be {@code null}
     * @param result computed result payload
     * @return success response
     */
    JsonRpcResponse success(@Nullable JsonNode id, JsonNode result);

    /**
     * Creates an error JSON-RPC response.
     *
     * @param id    request identifier; may be {@code null}
     * @param error mapped error payload
     * @return error response
     */
    JsonRpcResponse error(@Nullable JsonNode id, JsonRpcError error);
}
