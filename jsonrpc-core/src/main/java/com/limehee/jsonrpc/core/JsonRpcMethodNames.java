package com.limehee.jsonrpc.core;

/**
 * Method name rules shared by request validation, dispatch, registration, and request building.
 * <p>
 * JSON-RPC 2.0 defines a single naming rule: names beginning with {@code rpc.} are reserved for rpc-internal methods.
 * This library additionally rejects blank names. Callers decide how a violation is reported, so these checks only
 * answer the question and never throw.
 * </p>
 */
final class JsonRpcMethodNames {

    private JsonRpcMethodNames() {
    }

    /**
     * Returns whether a method name is blank (empty or whitespace only).
     *
     * @param method non-null method name
     * @return {@code true} when the name is blank
     */
    static boolean isBlank(String method) {
        return method.isBlank();
    }

    /**
     * Returns whether a method name uses the reserved {@code rpc.} namespace.
     *
     * @param method non-null method name
     * @return {@code true} when the name starts with {@link JsonRpcConstants#RESERVED_METHOD_PREFIX}
     */
    static boolean isReserved(String method) {
        return method.startsWith(JsonRpcConstants.RESERVED_METHOD_PREFIX);
    }

    /**
     * Returns whether a method name is rejected regardless of registration: blank names and names in the reserved
     * {@code rpc.} namespace.
     *
     * @param method non-null method name
     * @return {@code true} when the name must not be dispatched, registered, or built
     */
    static boolean isRejected(String method) {
        return isBlank(method) || isReserved(method);
    }
}
