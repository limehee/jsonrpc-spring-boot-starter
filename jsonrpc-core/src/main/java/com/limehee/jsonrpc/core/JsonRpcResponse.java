package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

/**
 * JSON-RPC response payload model.
 * <p>
 * Exactly one of {@code result} or {@code error} must be present.
 *
 * @param jsonrpc protocol version string
 * @param id request id echoed back to caller; may be {@code null}
 * @param result success payload; may be {@code null} when {@code error} is present
 * @param error error payload; may be {@code null} when {@code result} is present
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonRpcResponse(
        String jsonrpc,
        @JsonInclude(JsonInclude.Include.ALWAYS) @Nullable JsonNode id,
        @Nullable JsonNode result,
        @Nullable JsonRpcError error
) {

    /**
     * Validates canonical response invariants.
     *
     * @param jsonrpc protocol version string
     * @param id request id echoed back to caller; may be {@code null}
     * @param result success payload; must be non-null when {@code error} is {@code null}
     * @param error error payload; must be non-null when {@code result} is {@code null}
     * @throws IllegalArgumentException when both {@code result} and {@code error} are present or
     *                                  when both are absent
     */
    public JsonRpcResponse {
        boolean hasResult = result != null;
        boolean hasError = error != null;
        if (hasResult == hasError) {
            throw new IllegalArgumentException("Response must contain exactly one of result or error");
        }
    }

    /**
     * Creates a successful response.
     *
     * @param id request id; may be {@code null}
     * @param result success payload
     * @return success response
     */
    public static JsonRpcResponse success(@Nullable JsonNode id, JsonNode result) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, result, null);
    }

    /**
     * Creates an error response from code/message.
     *
     * @param id request id; may be {@code null}
     * @param code JSON-RPC error code
     * @param message JSON-RPC error message
     * @return error response
     */
    public static JsonRpcResponse error(@Nullable JsonNode id, int code, String message) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, JsonRpcError.of(code, message));
    }

    /**
     * Creates an error response from a prebuilt error object.
     *
     * @param id request id; may be {@code null}
     * @param error error payload
     * @return error response
     */
    public static JsonRpcResponse error(@Nullable JsonNode id, JsonRpcError error) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, error);
    }
}
