package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface JsonRpcResponseComposer {

    JsonRpcResponse success(@Nullable JsonNode id, JsonNode result);

    JsonRpcResponse error(@Nullable JsonNode id, JsonRpcError error);
}
