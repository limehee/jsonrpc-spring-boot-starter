package com.limehee.jsonrpc.core;

public record JsonRpcMethodRegistration(String method, JsonRpcMethodHandler handler) {

    public static JsonRpcMethodRegistration of(String method, JsonRpcMethodHandler handler) {
        return new JsonRpcMethodRegistration(method, handler);
    }
}
