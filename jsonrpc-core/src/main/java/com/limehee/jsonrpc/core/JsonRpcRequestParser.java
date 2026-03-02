package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;

/**
 * Parses a raw JSON tree into a normalized {@link JsonRpcRequest} model.
 */
public interface JsonRpcRequestParser {

    /**
     * Parses a single JSON-RPC request object.
     *
     * @param node raw JSON object node
     * @return parsed request model
     * @throws JsonRpcException when the payload cannot be interpreted as a request object
     */
    JsonRpcRequest parse(JsonNode node);
}
