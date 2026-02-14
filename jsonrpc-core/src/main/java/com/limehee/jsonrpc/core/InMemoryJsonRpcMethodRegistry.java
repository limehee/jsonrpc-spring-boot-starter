package com.limehee.jsonrpc.core;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryJsonRpcMethodRegistry implements JsonRpcMethodRegistry {

    private final Map<String, JsonRpcMethodHandler> handlers = new ConcurrentHashMap<>();
    private final JsonRpcMethodNamespacePolicy namespacePolicy;

    public InMemoryJsonRpcMethodRegistry() {
        this(JsonRpcMethodNamespacePolicy.DISALLOW_RPC_PREFIX);
    }

    public InMemoryJsonRpcMethodRegistry(JsonRpcMethodNamespacePolicy namespacePolicy) {
        this.namespacePolicy = namespacePolicy;
    }

    @Override
    public void register(String method, JsonRpcMethodHandler handler) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        if (namespacePolicy == JsonRpcMethodNamespacePolicy.DISALLOW_RPC_PREFIX
                && method.startsWith(JsonRpcConstants.RESERVED_METHOD_PREFIX)) {
            throw new IllegalArgumentException("methods starting with rpc. are reserved");
        }
        handlers.put(method, handler);
    }

    @Override
    public Optional<JsonRpcMethodHandler> find(String method) {
        return Optional.ofNullable(handlers.get(method));
    }
}
