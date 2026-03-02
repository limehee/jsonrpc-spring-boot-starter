package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;

/**
 * Default JSON-RPC request validator enforcing core protocol constraints.
 */
public class DefaultJsonRpcRequestValidator implements JsonRpcRequestValidator {

    /**
     * Validates protocol version, method presence, id shape, and params type.
     *
     * @param request parsed request model
     * @throws JsonRpcException when request violates JSON-RPC 2.0 constraints
     */
    @Override
    public void validate(JsonRpcRequest request) {
        if (request == null) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        }
        if (!JsonRpcConstants.VERSION.equals(request.jsonrpc())) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        }
        if (request.method() == null || request.method().isBlank()) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        }

        JsonNode id = request.id();
        if (request.idPresent() && id != null && !id.isNull() && !id.isTextual() && !id.isNumber()) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        }

        JsonNode params = request.params();
        if (params != null && !params.isArray() && !params.isObject()) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_PARAMS, JsonRpcConstants.MESSAGE_INVALID_PARAMS);
        }
    }
}
