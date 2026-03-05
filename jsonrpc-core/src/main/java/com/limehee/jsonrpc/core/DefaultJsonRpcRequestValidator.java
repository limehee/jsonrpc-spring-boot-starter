package com.limehee.jsonrpc.core;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Default JSON-RPC request validator enforcing core protocol constraints.
 */
public class DefaultJsonRpcRequestValidator implements JsonRpcRequestValidator {

    private final JsonRpcRequestValidationOptions options;

    /**
     * Creates a validator with default request-validation options.
     */
    public DefaultJsonRpcRequestValidator() {
        this(JsonRpcRequestValidationOptions.defaults());
    }

    /**
     * Creates a validator with explicit request-validation options.
     *
     * @param options request-validation options
     */
    public DefaultJsonRpcRequestValidator(JsonRpcRequestValidationOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Creates a validator with an explicit error-code policy for invalid {@code params} type.
     *
     * @param paramsTypeViolationCodePolicy policy selecting the error code for invalid {@code params} type violations
     */
    public DefaultJsonRpcRequestValidator(JsonRpcParamsTypeViolationCodePolicy paramsTypeViolationCodePolicy) {
        this(
            JsonRpcRequestValidationOptions.builder()
                .paramsTypeViolationCodePolicy(paramsTypeViolationCodePolicy)
                .build()
        );
    }

    /**
     * Validates protocol version, method presence, reserved method namespace, id shape, and params type.
     *
     * @param request parsed request model
     * @throws JsonRpcException when request violates JSON-RPC 2.0 constraints
     */
    @Override
    public void validate(JsonRpcRequest request) {
        if (request == null) {
            throw invalidRequest();
        }

        if (options.requireJsonRpcVersion20() && !JsonRpcConstants.VERSION.equals(request.jsonrpc())) {
            throw invalidRequest();
        }

        if (request.method() == null || request.method().isBlank()) {
            throw invalidRequest();
        }
        if (request.method().startsWith(JsonRpcConstants.RESERVED_METHOD_PREFIX)) {
            throw invalidRequest();
        }

        if (options.requireIdMember() && !request.idPresent()) {
            throw invalidRequest();
        }

        if (request.idPresent()) {
            validateId(request.id());
        }

        if (options.rejectResponseFields()) {
            JsonNode source = request.source();
            if (source != null && (source.has("result") || source.has("error"))) {
                throw invalidRequest();
            }
        }

        JsonNode params = request.params();
        if (params != null && !params.isArray() && !params.isObject()) {
            if (options.paramsTypeViolationCodePolicy() == JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST) {
                throw invalidRequest();
            }
            throw invalidParams();
        }
    }

    /**
     * Validates request {@code id} against configured ID rules.
     *
     * @param id request id node
     */
    private void validateId(@Nullable JsonNode id) {
        if (id == null || id.isNull()) {
            if (!options.allowNullId()) {
                throw invalidRequest();
            }
            return;
        }

        if (id.isString()) {
            if (!options.allowStringId()) {
                throw invalidRequest();
            }
            return;
        }

        if (id.isNumber()) {
            if (!options.allowNumericId()) {
                throw invalidRequest();
            }
            if (!options.allowFractionalId() && id.isFloatingPointNumber()) {
                throw invalidRequest();
            }
            return;
        }

        throw invalidRequest();
    }

    /**
     * Creates a standardized invalid-request exception.
     *
     * @return invalid-request exception
     */
    private JsonRpcException invalidRequest() {
        return new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
    }

    /**
     * Creates a standardized invalid-params exception.
     *
     * @return invalid-params exception
     */
    private JsonRpcException invalidParams() {
        return new JsonRpcException(JsonRpcErrorCode.INVALID_PARAMS, JsonRpcConstants.MESSAGE_INVALID_PARAMS);
    }
}
