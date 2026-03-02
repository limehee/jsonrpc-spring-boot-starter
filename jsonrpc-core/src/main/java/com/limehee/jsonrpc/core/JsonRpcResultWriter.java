package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * Serializes Java method results into JSON trees for JSON-RPC responses.
 */
public interface JsonRpcResultWriter {

    /**
     * Converts a Java value into a JSON node.
     *
     * @param value Java value returned by a method handler; may be {@code null}
     * @return JSON representation used as JSON-RPC {@code result}
     */
    JsonNode write(@Nullable Object value);
}
