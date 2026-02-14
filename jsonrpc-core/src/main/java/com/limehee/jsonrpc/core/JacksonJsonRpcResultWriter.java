package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

public class JacksonJsonRpcResultWriter implements JsonRpcResultWriter {

    private final ObjectMapper objectMapper;

    public JacksonJsonRpcResultWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode write(@Nullable Object value) {
        return objectMapper.valueToTree(value);
    }
}
