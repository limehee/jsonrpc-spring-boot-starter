package com.limehee.jsonrpc.core;

import java.util.Optional;

/**
 * Stores and resolves JSON-RPC method handlers by method name.
 */
public interface JsonRpcMethodRegistry {

    /**
     * Registers a handler for a method name.
     *
     * @param method  JSON-RPC method name
     * @param handler handler to register
     */
    void register(String method, JsonRpcMethodHandler handler);

    /**
     * Finds a handler by method name.
     *
     * @param method JSON-RPC method name
     * @return handler if registered, otherwise empty
     */
    Optional<JsonRpcMethodHandler> find(String method);
}
