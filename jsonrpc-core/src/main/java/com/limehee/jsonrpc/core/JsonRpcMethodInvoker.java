package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface JsonRpcMethodInvoker {

    JsonNode invoke(JsonRpcMethodHandler handler, @Nullable JsonNode params) throws Exception;
}
