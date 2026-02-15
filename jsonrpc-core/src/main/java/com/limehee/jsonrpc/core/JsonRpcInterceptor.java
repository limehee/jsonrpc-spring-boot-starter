package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface JsonRpcInterceptor {

    default void beforeValidate(JsonNode rawRequest) {
    }

    default void beforeInvoke(JsonRpcRequest request) {
    }

    default void afterInvoke(JsonRpcRequest request, JsonNode result) {
    }

    default void onError(@Nullable JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
    }
}
