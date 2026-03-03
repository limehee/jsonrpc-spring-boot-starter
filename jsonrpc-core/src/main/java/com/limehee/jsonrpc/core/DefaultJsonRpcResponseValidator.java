package com.limehee.jsonrpc.core;

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
        this.options = options;
    }

    /**
     * Validates response fields according to configured options.
     *
     * @param response parsed incoming response
     */
    @Override
    public void validate(JsonRpcIncomingResponse response) {
        if (response == null) {
            throw invalid("response must not be null");
        }

        if (options.requireJsonRpcVersion20() && !JsonRpcConstants.VERSION.equals(response.jsonrpc())) {
            throw invalid("response jsonrpc must be \"2.0\"");
        }

        if (options.requireResponseIdMember() && !response.idPresent()) {
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

        if (!options.allowRequestFieldsInResponse()) {
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
    private void validateId(JsonNode id) {
        if (id == null || id.isNull()) {
            if (!options.allowNullResponseId()) {
                throw invalid("response id must not be null");
            }
            return;
        }

        if (id.isTextual()) {
            if (!options.allowStringResponseId()) {
                throw invalid("response string id is not allowed");
            }
            return;
        }

        if (id.isNumber()) {
            if (!options.allowNumericResponseId()) {
                throw invalid("response numeric id is not allowed");
            }
            if (!options.allowFractionalResponseId() && id.isFloatingPointNumber()) {
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
    private void validateError(JsonNode error) {
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

        JsonNode message = error.get("message");
        if (options.requireStringErrorMessage()) {
            if (message == null || !message.isTextual()) {
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
}
