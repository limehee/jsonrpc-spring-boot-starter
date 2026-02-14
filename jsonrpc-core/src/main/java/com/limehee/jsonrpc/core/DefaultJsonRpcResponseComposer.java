package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJsonRpcResponseComposer implements JsonRpcResponseComposer {

    @Override
    public JsonRpcResponse success(JsonNode id, JsonNode result) {
        return JsonRpcResponse.success(id, result);
    }

    @Override
    public JsonRpcResponse error(JsonNode id, JsonRpcError error) {
        return JsonRpcResponse.error(id, error);
    }
}
