package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public class DefaultJsonRpcMethodInvoker implements JsonRpcMethodInvoker {

    @Override
    public JsonNode invoke(JsonRpcMethodHandler handler, @Nullable JsonNode params) {
        return handler.handle(params);
    }
}
