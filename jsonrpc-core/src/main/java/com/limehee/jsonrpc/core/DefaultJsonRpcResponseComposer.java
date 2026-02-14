package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public class DefaultJsonRpcResponseComposer implements JsonRpcResponseComposer {

    @Override
    public JsonRpcResponse success(@Nullable JsonNode id, JsonNode result) {
        return JsonRpcResponse.success(id, result);
    }

    @Override
    public JsonRpcResponse error(@Nullable JsonNode id, JsonRpcError error) {
        return JsonRpcResponse.error(id, error);
    }
}
