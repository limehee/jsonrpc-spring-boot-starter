package com.limehee.jsonrpc.core;

/**
 * Immutable method registration entry used for programmatic registration.
 *
 * @param method JSON-RPC method name
 * @param handler handler implementation for the method
 */
public record JsonRpcMethodRegistration(String method, JsonRpcMethodHandler handler) {

    /**
     * Creates a registration entry.
     *
     * @param method JSON-RPC method name
     * @param handler handler implementation for the method
     * @return registration entry
     */
    public static JsonRpcMethodRegistration of(String method, JsonRpcMethodHandler handler) {
        return new JsonRpcMethodRegistration(method, handler);
    }
}
