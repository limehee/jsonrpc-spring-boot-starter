package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
        String jsonrpc,
        @JsonInclude(JsonInclude.Include.ALWAYS) @Nullable JsonNode id,
        @Nullable JsonNode result,
        @Nullable JsonRpcError error
) {

    public JsonRpcResponse {
        boolean hasResult = result != null;
        boolean hasError = error != null;
        if (hasResult == hasError) {
            throw new IllegalArgumentException("Response must contain exactly one of result or error");
        }
    }

    public static JsonRpcResponse success(@Nullable JsonNode id, JsonNode result) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, result, null);
    }

    public static JsonRpcResponse error(@Nullable JsonNode id, int code, String message) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, JsonRpcError.of(code, message));
    }

    public static JsonRpcResponse error(@Nullable JsonNode id, JsonRpcError error) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, error);
    }
}
