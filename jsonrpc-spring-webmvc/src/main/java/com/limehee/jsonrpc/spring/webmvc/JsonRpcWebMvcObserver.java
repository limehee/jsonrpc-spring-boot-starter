package com.limehee.jsonrpc.spring.webmvc;

import com.limehee.jsonrpc.core.JsonRpcResponse;

import java.util.List;

public interface JsonRpcWebMvcObserver {

    JsonRpcWebMvcObserver NO_OP = new JsonRpcWebMvcObserver() {
    };

    static JsonRpcWebMvcObserver noOp() {
        return NO_OP;
    }

    default void onParseError() {
    }

    default void onRequestTooLarge(int actualBytes, int maxBytes) {
    }

    default void onSingleResponse(JsonRpcResponse response) {
    }

    default void onBatchResponse(int requestCount, List<JsonRpcResponse> responses) {
    }

    default void onNotificationOnly(boolean batch, int requestCount) {
    }
}
