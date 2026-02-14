package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcMethodInvoker {

    JsonNode invoke(JsonRpcMethodHandler handler, JsonNode params) throws Exception;
}
