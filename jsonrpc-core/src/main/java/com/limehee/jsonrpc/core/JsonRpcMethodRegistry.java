package com.limehee.jsonrpc.core;

import java.util.Optional;

public interface JsonRpcMethodRegistry {

    void register(String method, JsonRpcMethodHandler handler);

    Optional<JsonRpcMethodHandler> find(String method);
}
