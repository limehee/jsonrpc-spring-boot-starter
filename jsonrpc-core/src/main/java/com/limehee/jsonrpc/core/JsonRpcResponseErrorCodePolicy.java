package com.limehee.jsonrpc.core;

/**
 * Policy controlling accepted integer ranges for incoming response {@code error.code}.
 */
public enum JsonRpcResponseErrorCodePolicy {

    /**
     * Accept any integer.
     */
    ANY_INTEGER,

    /**
     * Accept only JSON-RPC standard error codes.
     */
    STANDARD_ONLY,

    /**
     * Accept standard error codes and server-error reserved range ({@code -32099..-32000}).
     */
    STANDARD_OR_SERVER_ERROR_RANGE,

    /**
     * Accept only values inside a user-defined custom range.
     */
    CUSTOM_RANGE
}
