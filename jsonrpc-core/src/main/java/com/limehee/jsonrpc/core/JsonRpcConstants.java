package com.limehee.jsonrpc.core;

/**
 * Core protocol constants and default error messages.
 */
public final class JsonRpcConstants {

    /** JSON-RPC protocol version supported by this library. */
    public static final String VERSION = "2.0";
    /** Reserved method namespace defined by the JSON-RPC specification. */
    public static final String RESERVED_METHOD_PREFIX = "rpc.";

    /** Default parse error message. */
    public static final String MESSAGE_PARSE_ERROR = "Parse error";
    /** Default invalid request message. */
    public static final String MESSAGE_INVALID_REQUEST = "Invalid Request";
    /** Default method-not-found message. */
    public static final String MESSAGE_METHOD_NOT_FOUND = "Method not found";
    /** Default invalid params message. */
    public static final String MESSAGE_INVALID_PARAMS = "Invalid params";
    /** Default internal error message. */
    public static final String MESSAGE_INTERNAL_ERROR = "Internal error";

    /**
     * Utility class.
     */
    private JsonRpcConstants() {
    }
}
