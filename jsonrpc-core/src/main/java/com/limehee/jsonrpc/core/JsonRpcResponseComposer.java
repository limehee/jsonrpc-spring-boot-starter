package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcResponseComposer {

    JsonRpcResponse success(JsonNode id, JsonNode result);

    JsonRpcResponse error(JsonNode id, JsonRpcError error);
}
