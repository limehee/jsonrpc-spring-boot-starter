package com.limehee.jsonrpc.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory {@link JsonRpcMethodRegistry} implementation.
 */
public class InMemoryJsonRpcMethodRegistry implements JsonRpcMethodRegistry {

    private final Map<String, JsonRpcMethodHandler> handlers = new ConcurrentHashMap<>();
    private final JsonRpcMethodRegistrationConflictPolicy conflictPolicy;

    /**
     * Creates registry with {@link JsonRpcMethodRegistrationConflictPolicy#REJECT}.
     */
    public InMemoryJsonRpcMethodRegistry() {
        this(JsonRpcMethodRegistrationConflictPolicy.REJECT);
    }

    /**
     * Creates registry with an explicit conflict policy.
     *
     * @param conflictPolicy duplicate method registration policy
     */
    public InMemoryJsonRpcMethodRegistry(JsonRpcMethodRegistrationConflictPolicy conflictPolicy) {
        this.conflictPolicy = Objects.requireNonNull(conflictPolicy, "conflictPolicy");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(String method, JsonRpcMethodHandler handler) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        }
        if (method.startsWith(JsonRpcConstants.RESERVED_METHOD_PREFIX)) {
            throw new IllegalArgumentException("methods starting with rpc. are reserved");
        }
        if (conflictPolicy == JsonRpcMethodRegistrationConflictPolicy.REJECT) {
            JsonRpcMethodHandler existing = handlers.putIfAbsent(method, handler);
            if (existing != null) {
                throw new IllegalStateException("method is already registered: " + method);
            }
            return;
        }
        handlers.put(method, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<JsonRpcMethodHandler> find(String method) {
        return Optional.ofNullable(handlers.get(method));
    }
}
