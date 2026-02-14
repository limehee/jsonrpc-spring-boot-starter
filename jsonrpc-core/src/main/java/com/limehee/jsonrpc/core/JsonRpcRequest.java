package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public record JsonRpcRequest(String jsonrpc, JsonNode id, String method, JsonNode params, boolean idPresent) {

    public boolean isNotification() {
        return !idPresent;
    }
}
