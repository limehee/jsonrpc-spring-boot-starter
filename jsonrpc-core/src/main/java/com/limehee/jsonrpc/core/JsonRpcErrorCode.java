package com.limehee.jsonrpc.core;

/**
 * Standard JSON-RPC 2.0 error code constants.
 */
public final class JsonRpcErrorCode {

    /**
     * JSON parsing error.
     */
    public static final int PARSE_ERROR = -32700;
    /**
     * Invalid request object structure.
     */
    public static final int INVALID_REQUEST = -32600;
    /**
     * Unknown method name.
     */
    public static final int METHOD_NOT_FOUND = -32601;
    /**
     * Invalid method parameters.
     */
    public static final int INVALID_PARAMS = -32602;
    /**
     * Generic server-side error.
     */
    public static final int INTERNAL_ERROR = -32603;

    /**
     * Utility class.
     */
    private JsonRpcErrorCode() {
    }
}
