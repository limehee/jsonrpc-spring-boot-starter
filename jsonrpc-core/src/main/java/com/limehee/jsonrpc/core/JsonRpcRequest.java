package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public record JsonRpcRequest(
        @Nullable String jsonrpc,
        @Nullable JsonNode id,
        @Nullable String method,
        @Nullable JsonNode params,
        boolean idPresent
) {

    public boolean isNotification() {
        return !idPresent;
    }
}
