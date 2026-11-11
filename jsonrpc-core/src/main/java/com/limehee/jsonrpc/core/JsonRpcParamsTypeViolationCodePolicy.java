package com.limehee.jsonrpc.core;

/**
 * Policy controlling which JSON-RPC error code is emitted when {@code params} has an invalid type.
 */
public enum JsonRpcParamsTypeViolationCodePolicy {
    /**
     * Maps invalid {@code params} type to {@code -32602 Invalid params}.
     * <p>
     * Not the JSON-RPC 2.0 classification (a non-structured {@code params} makes the Request object invalid); kept for
     * clients that expect the pre-0.3 behavior.
     * </p>
     */
    INVALID_PARAMS,
    /**
     * Maps invalid {@code params} type to {@code -32600 Invalid Request} (default).
     * <p>
     * JSON-RPC 2.0 section 4 requires {@code params}, when present, to be a structured value, and section 5.1 defines
     * {@code -32600} as "The JSON sent is not a valid Request object".
     * </p>
     */
    INVALID_REQUEST
}
