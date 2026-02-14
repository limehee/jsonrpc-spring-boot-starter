package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcInterceptor {

    default void beforeValidate(JsonNode rawRequest) {
    }

    default void beforeInvoke(JsonRpcRequest request) {
    }

    default void afterInvoke(JsonRpcRequest request, JsonNode result) {
    }

    default void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
    }
}
