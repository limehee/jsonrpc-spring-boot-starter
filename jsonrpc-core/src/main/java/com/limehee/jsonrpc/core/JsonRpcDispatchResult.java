package com.limehee.jsonrpc.core;

import java.util.List;
import java.util.Optional;

public final class JsonRpcDispatchResult {

    private final boolean batch;
    private final List<JsonRpcResponse> responses;

    private JsonRpcDispatchResult(boolean batch, List<JsonRpcResponse> responses) {
        this.batch = batch;
        this.responses = List.copyOf(responses);
    }

    public static JsonRpcDispatchResult single(Optional<JsonRpcResponse> response) {
        return new JsonRpcDispatchResult(false, response.stream().toList());
    }

    public static JsonRpcDispatchResult single(JsonRpcResponse response) {
        return new JsonRpcDispatchResult(false, List.of(response));
    }

    public static JsonRpcDispatchResult batch(List<JsonRpcResponse> responses) {
        return new JsonRpcDispatchResult(true, responses);
    }

    public boolean isBatch() {
        return batch;
    }

    public boolean hasResponse() {
        return !responses.isEmpty();
    }

    public List<JsonRpcResponse> responses() {
        return responses;
    }

    public Optional<JsonRpcResponse> singleResponse() {
        if (batch || responses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(responses.get(0));
    }
}
