package com.limehee.jsonrpc.core;

/**
 * Wraps unexpected runtime failures thrown by interceptor callbacks.
 */
public final class JsonRpcInterceptorExecutionException extends RuntimeException {

    /**
     * Creates an exception wrapper.
     *
     * @param cause original interceptor exception
     */
    public JsonRpcInterceptorExecutionException(Throwable cause) {
        super(cause);
    }
}
