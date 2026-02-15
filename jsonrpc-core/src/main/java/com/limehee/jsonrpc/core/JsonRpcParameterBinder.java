package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface JsonRpcParameterBinder {

    <T> T bind(@Nullable JsonNode params, Class<T> targetType);
}
