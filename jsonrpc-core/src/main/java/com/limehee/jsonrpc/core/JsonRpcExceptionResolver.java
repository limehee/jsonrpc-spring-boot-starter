package com.limehee.jsonrpc.core;

/**
 * Maps thrown exceptions to JSON-RPC error payloads.
 */
public interface JsonRpcExceptionResolver {

    /**
     * Resolves an exception into a JSON-RPC error object.
     *
     * @param throwable exception raised during request handling
     * @return mapped JSON-RPC error
     */
    JsonRpcError resolve(Throwable throwable);
}
