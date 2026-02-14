package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJsonRpcRequestParser implements JsonRpcRequestParser {

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
