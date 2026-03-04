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
            throw invalid("response must not be null");
        }

        if (options.requireJsonRpcVersion20() && !JsonRpcConstants.VERSION.equals(response.jsonrpc())) {
            throw invalid("response jsonrpc must be \"2.0\"");
        }

        if (options.requireIdMember() && !response.idPresent()) {
            throw invalid("response id must be present");
        }

        if (response.idPresent()) {
            validateId(response.id());
        }

        if (options.requireExclusiveResultOrError()) {
            boolean hasResult = response.resultPresent();
            boolean hasError = response.errorPresent();
            if (hasResult == hasError) {
                throw invalid("response must contain exactly one of result or error");
            }
        }

        if (options.rejectRequestFields()) {
            JsonNode source = response.source();
            if (source == null || source.has("method") || source.has("params")) {
                throw invalid("response must not contain request fields method/params");
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
                throw invalid("response id must not be null");
            }
            return;
        }

        if (id.isString()) {
            if (!options.allowStringId()) {
                throw invalid("response string id is not allowed");
            }
            return;
        }

        if (id.isNumber()) {
            if (!options.allowNumericId()) {
                throw invalid("response numeric id is not allowed");
            }
            if (!options.allowFractionalId() && id.isFloatingPointNumber()) {
                throw invalid("response fractional numeric id is not allowed");
            }
            return;
        }

        throw invalid("response id must be string, number, or null");
    }

    /**
     * Validates the response {@code error} object and its required members.
     *
     * @param error error node
     */
    private void validateError(@Nullable JsonNode error) {
        if (options.requireErrorObjectWhenPresent() && (error == null || !error.isObject())) {
            throw invalid("response error must be an object");
        }

        if (error == null || !error.isObject()) {
            return;
        }

        JsonNode code = error.get("code");
        if (options.requireIntegerErrorCode()) {
            if (code == null || !code.isNumber() || code.isFloatingPointNumber()) {
                throw invalid("response error.code must be an integer");
            }
        }
        if (code != null && code.isNumber() && !code.isFloatingPointNumber()) {
            validateErrorCodePolicy(code.intValue());
        }

        JsonNode message = error.get("message");
        if (options.requireStringErrorMessage()) {
            if (message == null || !message.isString()) {
                throw invalid("response error.message must be a string");
            }
        }
    }

    /**
     * Creates a standardized invalid-response exception.
     *
     * @param message detail message
     * @return invalid-request exception
     */
    private JsonRpcException invalid(String message) {
        return new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, message);
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
                throw invalid("response error.code must be one of JSON-RPC standard codes");
            }
            return;
        }
        if (policy == JsonRpcResponseErrorCodePolicy.STANDARD_OR_SERVER_ERROR_RANGE) {
            if (!isStandardErrorCode(code) && !isServerErrorRangeCode(code)) {
                throw invalid("response error.code must be standard or server-error range");
            }
            return;
        }
        if (policy == JsonRpcResponseErrorCodePolicy.CUSTOM_RANGE) {
            Integer min = options.errorCodeRangeMin();
            Integer max = options.errorCodeRangeMax();
            if (min == null || max == null) {
                throw invalid("response error.code custom range is not configured");
            }
            if (code < min || code > max) {
                throw invalid("response error.code is outside configured custom range");
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
