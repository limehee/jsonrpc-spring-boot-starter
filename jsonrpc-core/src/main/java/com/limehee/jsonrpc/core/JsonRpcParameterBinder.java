package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcParameterBinder {

    <T> T bind(JsonNode params, Class<T> targetType);
}
