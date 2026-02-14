package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonJsonRpcParameterBinder implements JsonRpcParameterBinder {

    private final ObjectMapper objectMapper;

    public JacksonJsonRpcParameterBinder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T bind(JsonNode params, Class<T> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType must not be null");
        }
        if (targetType == JsonNode.class) {
            return targetType.cast(params);
        }

        try {
            if (params == null || params.isNull()) {
                return objectMapper.convertValue(null, targetType);
            }
            return objectMapper.treeToValue(params, targetType);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new IllegalArgumentException(JsonRpcConstants.MESSAGE_INVALID_PARAMS, ex);
        }
    }
}
