package com.limehee.jsonrpc.core;

import java.util.Objects;
import tools.jackson.databind.JsonNode;

/**
 * Default JSON-RPC request validator enforcing core protocol constraints.
 */
public class DefaultJsonRpcRequestValidator implements JsonRpcRequestValidator {

    private final JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy;

    /**
     * Creates a validator using {@link JsonRpcParamsTypeViolationCodePolicy#INVALID_PARAMS} for invalid {@code params}
     * type violations.
     */
    public DefaultJsonRpcRequestValidator() {
        this(JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);
    }

    /**
     * Creates a validator with an explicit error-code policy for invalid {@code params} type.
     *
     * @param paramsTypeViolationCodePolicy policy selecting the error code for invalid {@code params} type violations
     */
    public DefaultJsonRpcRequestValidator(JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy) {
        this.paramsTypeViolationCodePolicy = Objects.requireNonNull(
            paramsTypeViolationCodePolicy,
            "paramsTypeViolationCodePolicy"
        );
    }

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
        if (request.idPresent() && id != null && !id.isNull() && !id.isString() && !id.isNumber()) {
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        }

        JsonNode params = request.params();
        if (params != null && !params.isArray() && !params.isObject()) {
            if (paramsTypeViolationCodePolicy == JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST) {
                throw new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
            }
            throw new JsonRpcException(JsonRpcErrorCode.INVALID_PARAMS, JsonRpcConstants.MESSAGE_INVALID_PARAMS);
        }
    }
}
