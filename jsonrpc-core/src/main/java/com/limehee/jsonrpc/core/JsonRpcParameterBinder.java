package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Binds raw JSON-RPC parameters to Java target types.
 */
public interface JsonRpcParameterBinder {

    /**
     * Converts a params node into a typed Java value.
     *
     * @param params     JSON-RPC params value; may be {@code null}
     * @param targetType Java target class for conversion
     * @param <T>        target value type
     * @return converted value
     * @throws JsonRpcException when parameter conversion fails
     */
    <T> T bind(@Nullable JsonNode params, Class<T> targetType);
}
