package com.limehee.jsonrpc.spring.webmvc;

import com.limehee.jsonrpc.core.JsonRpcResponse;
import java.util.List;

/**
 * Observer hook interface for transport-level JSON-RPC events emitted by the WebMVC endpoint.
 * <p>
 * Implementations can collect metrics, auditing information, or diagnostics without changing dispatch behavior. All
 * methods are optional and default to no-op.
 * </p>
 */
public interface JsonRpcWebMvcObserver {

    /**
     * Shared no-op observer instance used when observation is not configured.
     */
    JsonRpcWebMvcObserver NO_OP = new JsonRpcWebMvcObserver() {
    };

    /**
     * Returns a reusable no-op observer.
     *
     * @return observer that ignores all callbacks
     */
    static JsonRpcWebMvcObserver noOp() {
        return NO_OP;
    }

    /**
     * Called when the HTTP payload cannot be parsed into a JSON value.
     */
    default void onParseError() {
    }

    /**
     * Called when the request payload exceeds configured transport limits.
     *
     * @param actualBytes actual body size in bytes
     * @param maxBytes    configured maximum accepted body size in bytes
     */
    default void onRequestTooLarge(int actualBytes, int maxBytes) {
    }

    /**
     * Called when a single request produced a single JSON-RPC response.
     *
     * @param response response generated for the request
     */
    default void onSingleResponse(JsonRpcResponse response) {
    }

    /**
     * Called when a batch request produced one or more JSON-RPC responses.
     *
     * @param requestCount number of entries in the incoming batch payload
     * @param responses    response payload entries emitted for that batch
     */
    default void onBatchResponse(int requestCount, List<JsonRpcResponse> responses) {
    }

    /**
     * Called when request handling produced no JSON-RPC payload (notification-only path).
     *
     * @param batch        {@code true} when the incoming payload was a batch array
     * @param requestCount number of requests in the incoming payload
     */
    default void onNotificationOnly(boolean batch, int requestCount) {
    }
}
