package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJsonRpcMethodInvoker implements JsonRpcMethodInvoker {

    @Override
    public JsonNode invoke(JsonRpcMethodHandler handler, JsonNode params) {
        return handler.handle(params);
    }
}
