package com.limehee.jsonrpc.core;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;

public class JacksonJsonRpcParameterBinder implements JsonRpcParameterBinder {

    private final ObjectMapper objectMapper;

    public JacksonJsonRpcParameterBinder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T bind(@Nullable JsonNode params, Class<T> targetType) {
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
        } catch (JacksonException | IllegalArgumentException ex) {
            throw new JsonRpcException(
                    JsonRpcErrorCode.INVALID_PARAMS,
                    JsonRpcConstants.MESSAGE_INVALID_PARAMS,
                    null,
                    ex
            );
        }
    }
}
