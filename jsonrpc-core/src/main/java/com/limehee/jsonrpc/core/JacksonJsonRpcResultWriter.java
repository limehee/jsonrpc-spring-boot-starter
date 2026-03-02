package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

/**
 * {@link JsonRpcResultWriter} backed by Jackson tree conversion.
 */
public class JacksonJsonRpcResultWriter implements JsonRpcResultWriter {

    private final ObjectMapper objectMapper;

    /**
     * Creates a writer.
     *
     * @param objectMapper Jackson mapper used for value-to-tree conversion
     */
    public JacksonJsonRpcResultWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonNode write(@Nullable Object value) {
        return objectMapper.valueToTree(value);
    }
}
