package com.limehee.jsonrpc.core;

/**
 * Policy controlling which JSON-RPC error code is emitted when {@code params} has an invalid type.
 */
public enum JsonRpcParamsTypeViolationCodePolicy {
    /**
     * Maps invalid {@code params} type to {@code -32602 Invalid params}.
     */
    INVALID_PARAMS,
    /**
     * Maps invalid {@code params} type to {@code -32600 Invalid Request}.
     */
    INVALID_REQUEST
}
