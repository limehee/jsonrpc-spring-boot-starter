package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
        String jsonrpc,
        @JsonInclude(JsonInclude.Include.ALWAYS) JsonNode id,
        JsonNode result,
        JsonRpcError error
) {

    public JsonRpcResponse {
        boolean hasResult = result != null;
        boolean hasError = error != null;
        if (hasResult == hasError) {
            throw new IllegalArgumentException("Response must contain exactly one of result or error");
        }
    }

    public static JsonRpcResponse success(JsonNode id, JsonNode result) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, result, null);
    }

    public static JsonRpcResponse error(JsonNode id, int code, String message) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, JsonRpcError.of(code, message));
    }

    public static JsonRpcResponse error(JsonNode id, JsonRpcError error) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, error);
    }
}
