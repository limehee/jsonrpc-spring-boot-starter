package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonJsonRpcResultWriter implements JsonRpcResultWriter {

    private final ObjectMapper objectMapper;

    public JacksonJsonRpcResultWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode write(Object value) {
        return objectMapper.valueToTree(value);
    }
}
