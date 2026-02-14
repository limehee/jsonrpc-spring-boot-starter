package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface JsonRpcMethodHandler {

    JsonNode handle(@Nullable JsonNode params);
}
