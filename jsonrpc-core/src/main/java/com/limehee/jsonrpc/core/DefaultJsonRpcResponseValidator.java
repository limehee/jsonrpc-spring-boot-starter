package com.limehee.jsonrpc.core;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Default validator for parsed incoming JSON-RPC responses.
 */
public class DefaultJsonRpcResponseValidator implements JsonRpcResponseValidator {

    private final JsonRpcResponseValidationOptions options;

    /**
     * Creates a validator with default RFC-aligned options.
     */
    public DefaultJsonRpcResponseValidator() {
        this(JsonRpcResponseValidationOptions.defaults());
    }

    /**
     * Creates a validator with explicit validation options.
     *
     * @param options response validation options
     */
    public DefaultJsonRpcResponseValidator(JsonRpcResponseValidationOptions options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(JsonRpcIncomingResponse response) {
        if (response == null) {
            throw invalidRequest();
        }

        if (options.requireJsonRpcVersion20() && !JsonRpcConstants.VERSION.equals(response.jsonrpc())) {
            throw invalidRequest();
        }

        if (options.requireIdMember() && !response.idPresent()) {
            throw invalidRequest();
        }

        if (response.idPresent()) {
            validateId(response.id());
        }

        if (options.requireExclusiveResultOrError()) {
            boolean hasResult = response.resultPresent();
            boolean hasError = response.errorPresent();
            if (hasResult == hasError) {
                throw invalidRequest();
            }
        }

        if (options.rejectRequestFields()) {
            JsonNode source = response.source();
            if (source != null && (source.has("method") || source.has("params"))) {
                throw invalidRequest();
            }
        }

        if (response.errorPresent()) {
            validateError(response.error());
        }
    }

    /**
     * Validates response {@code id} against configured ID rules.
     *
     * @param id response id node
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
     * Validates the response {@code error} object and its required members.
     *
     * @param error error node
     */
    private void validateError(@Nullable JsonNode error) {
        if (options.requireErrorObjectWhenPresent() && (error == null || !error.isObject())) {
            throw invalidRequest();
        }

        if (error == null || !error.isObject()) {
            return;
        }

        JsonNode code = error.get("code");
        if (options.requireIntegerErrorCode()) {
            if (code == null || !code.isNumber() || code.isFloatingPointNumber()) {
                throw invalidRequest();
            }
        }
        if (code != null && code.isNumber() && !code.isFloatingPointNumber()) {
            validateErrorCodePolicy(code.intValue());
        }

        JsonNode message = error.get("message");
        if (options.requireStringErrorMessage()) {
            if (message == null || !message.isString()) {
                throw invalidRequest();
            }
        }
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
     * Validates integer {@code error.code} value against configured range policy.
     *
     * @param code integer error code
     */
    private void validateErrorCodePolicy(int code) {
        JsonRpcResponseErrorCodePolicy policy = options.errorCodePolicy();
        if (policy == JsonRpcResponseErrorCodePolicy.ANY_INTEGER) {
            return;
        }
        if (policy == JsonRpcResponseErrorCodePolicy.STANDARD_ONLY) {
            if (!isStandardErrorCode(code)) {
                throw invalidRequest();
            }
            return;
        }
        if (policy == JsonRpcResponseErrorCodePolicy.STANDARD_OR_SERVER_ERROR_RANGE) {
            if (!isStandardErrorCode(code) && !isServerErrorRangeCode(code)) {
                throw invalidRequest();
            }
            return;
        }
        if (policy == JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE) {
            Integer min = options.errorCodeRangeMin();
            Integer max = options.errorCodeRangeMax();
            if (min == null || max == null) {
                throw invalidRequest();
            }
            if (code < min || code > max) {
                throw invalidRequest();
            }
        }
    }

    /**
     * Checks whether a code is one of the JSON-RPC standard error codes.
     *
     * @param code integer error code
     * @return {@code true} when code is standard
     */
    private boolean isStandardErrorCode(int code) {
        return code == JsonRpcErrorCode.PARSE_ERROR
            || code == JsonRpcErrorCode.INVALID_REQUEST
            || code == JsonRpcErrorCode.METHOD_NOT_FOUND
            || code == JsonRpcErrorCode.INVALID_PARAMS
            || code == JsonRpcErrorCode.INTERNAL_ERROR;
    }

    /**
     * Checks whether a code belongs to the JSON-RPC server-error reserved range.
     *
     * @param code integer error code
     * @return {@code true} when code is within {@code -32099..-32000}
     */
    private boolean isServerErrorRangeCode(int code) {
        return code >= -32099 && code <= -32000;
    }
}
