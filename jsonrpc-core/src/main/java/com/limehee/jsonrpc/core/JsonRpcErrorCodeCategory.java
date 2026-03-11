package com.limehee.jsonrpc.core;

/**
 * Semantic category for an integer JSON-RPC {@code error.code}.
 */
public enum JsonRpcErrorCodeCategory {

    /**
     * One of the JSON-RPC 2.0 standard error codes.
     */
    STANDARD,

    /**
     * Value inside the server-reserved range {@code -32099..-32000}.
     */
    SERVER_RESERVED_RANGE,

    /**
     * Any integer outside the standard set and the server-reserved range.
     * <p>
     * This category does not mean the code is invalid. It only means the code is application-defined or otherwise
     * outside the standard and reserved sets.
     * </p>
     */
    CUSTOM
}
