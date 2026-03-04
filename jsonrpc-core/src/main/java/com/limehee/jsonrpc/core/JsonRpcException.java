package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Runtime exception representing JSON-RPC domain errors.
 * <p>
 * Unlike generic runtime exceptions, this exception carries a JSON-RPC error code and optional structured error data
 * that can be propagated to clients.
 */
public class JsonRpcException extends RuntimeException {

    /**
     * JSON-RPC error code associated with this exception.
     */
    private final int code;

    /**
     * Optional JSON-RPC {@code error.data} payload associated with this exception.
     */
    private final @Nullable JsonNode data;

    /**
     * Creates an exception with code/message and no data/cause.
     *
     * @param code    JSON-RPC error code
     * @param message human-readable message
     */
    public JsonRpcException(int code, String message) {
        this(code, message, null, null);
    }

    /**
     * Creates an exception with code/message/data and no cause.
     *
     * @param code    JSON-RPC error code
     * @param message human-readable message
     * @param data    optional error data payload
     */
    public JsonRpcException(int code, String message, @Nullable JsonNode data) {
        this(code, message, data, null);
    }

    /**
     * Creates an exception with code/message/data/cause.
     *
     * @param code    JSON-RPC error code
     * @param message human-readable message
     * @param data    optional error data payload
     * @param cause   original cause
     */
    public JsonRpcException(int code, String message, @Nullable JsonNode data, @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
        this.data = data;
    }

    /**
     * Returns the JSON-RPC error code.
     *
     * @return error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns optional structured error data.
     *
     * @return error data, or {@code null} when absent
     */
    public @Nullable JsonNode getData() {
        return data;
    }
}
