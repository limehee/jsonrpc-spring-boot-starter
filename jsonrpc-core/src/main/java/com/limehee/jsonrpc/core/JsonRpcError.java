package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcError(int code, String message, @Nullable JsonNode data) {

    public static JsonRpcError of(int code, String message) {
        return new JsonRpcError(code, message, null);
    }
}
