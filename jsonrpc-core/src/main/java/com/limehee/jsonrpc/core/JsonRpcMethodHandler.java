package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface JsonRpcMethodHandler {

    JsonNode handle(JsonNode params);
}
