package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;

/**
 * Default parser that extracts JSON-RPC request fields from an object node.
 */
public class DefaultJsonRpcRequestParser implements JsonRpcRequestParser {

    /**
     * Parses request fields from a raw node.
     *
     * @param node raw JSON node expected to be an object
     * @return parsed request
     * @throws JsonRpcException when the node is not a valid object request container
     */
    @Override
    public JsonRpcRequest parse(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        }

        JsonNode jsonrpcNode = node.get("jsonrpc");
        String jsonrpc = jsonrpcNode != null && jsonrpcNode.isTextual() ? jsonrpcNode.asText() : null;

        JsonNode methodNode = node.get("method");
        String method = methodNode != null && methodNode.isTextual() ? methodNode.asText() : null;

        JsonNode id = node.get("id");
        boolean idPresent = node.has("id");

        JsonNode params = node.get("params");

        return new JsonRpcRequest(jsonrpc, id, method, params, idPresent);
    }
}
