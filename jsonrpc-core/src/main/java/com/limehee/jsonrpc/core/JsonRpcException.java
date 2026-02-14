package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonRpcException extends RuntimeException {

    private final int code;
    private final JsonNode data;

    public JsonRpcException(int code, String message) {
        this(code, message, null, null);
    }

    public JsonRpcException(int code, String message, JsonNode data) {
        this(code, message, data, null);
    }

    public JsonRpcException(int code, String message, JsonNode data, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public JsonNode getData() {
        return data;
    }
}
