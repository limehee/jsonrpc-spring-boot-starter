package com.limehee.jsonrpc.core;

public final class JsonRpcConstants {

    public static final String VERSION = "2.0";
    public static final String RESERVED_METHOD_PREFIX = "rpc.";

    public static final String MESSAGE_PARSE_ERROR = "Parse error";
    public static final String MESSAGE_INVALID_REQUEST = "Invalid Request";
    public static final String MESSAGE_METHOD_NOT_FOUND = "Method not found";
    public static final String MESSAGE_INVALID_PARAMS = "Invalid params";
    public static final String MESSAGE_INTERNAL_ERROR = "Internal error";

    private JsonRpcConstants() {
    }
}
