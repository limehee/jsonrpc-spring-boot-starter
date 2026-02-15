package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public class JsonRpcException extends RuntimeException {

    private final int code;
    private final @Nullable JsonNode data;

    public JsonRpcException(int code, String message) {
        this(code, message, null, null);
    }

    public JsonRpcException(int code, String message, @Nullable JsonNode data) {
        this(code, message, data, null);
    }

    public JsonRpcException(int code, String message, @Nullable JsonNode data, @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public @Nullable JsonNode getData() {
        return data;
    }
}
