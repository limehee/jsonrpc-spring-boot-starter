package com.limehee.jsonrpc.core;

import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Builder for composing outbound JSON-RPC request and notification payloads.
 * <p>
 * This builder produces transport-ready Jackson nodes rather than parsed dispatcher models. It is intended for code
 * that needs to send JSON-RPC payloads to another endpoint.
 * </p>
 */
public final class JsonRpcRequestBuilder {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    private final String method;
    private final boolean notification;

    private @Nullable JsonNode id;
    private boolean idConfigured;
    private @Nullable JsonNode params;
    private boolean paramsConfigured;

    private JsonRpcRequestBuilder(String method, boolean notification) {
        this.method = validateMethod(method);
        this.notification = notification;
    }

    /**
     * Creates a builder for a request that must include an {@code id}.
     *
     * @param method JSON-RPC method name
     * @return request builder
     * @throws NullPointerException if {@code method} is {@code null}
     * @throws IllegalArgumentException if {@code method} is blank or uses the reserved {@code rpc.*} namespace
     */
    public static JsonRpcRequestBuilder request(String method) {
        return new JsonRpcRequestBuilder(method, false);
    }

    /**
     * Creates a builder for a notification that must not include an {@code id}.
     *
     * @param method JSON-RPC method name
     * @return notification builder
     * @throws NullPointerException if {@code method} is {@code null}
     * @throws IllegalArgumentException if {@code method} is blank or uses the reserved {@code rpc.*} namespace
     */
    public static JsonRpcRequestBuilder notification(String method) {
        return new JsonRpcRequestBuilder(method, true);
    }

    /**
     * Sets a numeric {@code id}.
     *
     * @param value numeric request id
     * @return this builder
     * @throws IllegalStateException if this builder represents a notification or the id has already been configured
     */
    public JsonRpcRequestBuilder id(long value) {
        return assignId(NODE_FACTORY.numberNode(value));
    }

    /**
     * Sets a string {@code id}.
     *
     * @param value string request id
     * @return this builder
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalStateException if this builder represents a notification or the id has already been configured
     */
    public JsonRpcRequestBuilder id(String value) {
        return assignId(NODE_FACTORY.stringNode(Objects.requireNonNull(value, "value")));
    }

    /**
     * Sets an explicit JSON {@code id} node.
     *
     * @param value JSON id node; must be string, number, or null
     * @return this builder
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not a JSON-RPC-compatible id node
     * @throws IllegalStateException if this builder represents a notification or the id has already been configured
     */
    public JsonRpcRequestBuilder id(JsonNode value) {
        Objects.requireNonNull(value, "value");
        if (!value.isString() && !value.isNumber() && !value.isNull()) {
            throw new IllegalArgumentException("id must be a string, number, or null JSON node");
        }
        return assignId(value);
    }

    /**
     * Sets an explicit {@code null} id.
     *
     * @return this builder
     * @throws IllegalStateException if this builder represents a notification or the id has already been configured
     */
    public JsonRpcRequestBuilder nullId() {
        return assignId(NODE_FACTORY.nullNode());
    }

    /**
     * Sets {@code params} from a prebuilt JSON node.
     * <p>
     * The provided node is snapshotted when assigned so that later caller-side mutations do not leak into the
     * builder's internal state.
     * </p>
     *
     * @param value params node; must be an object or array
     * @return this builder
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is not an object or array
     * @throws IllegalStateException if params have already been configured on this builder
     */
    public JsonRpcRequestBuilder params(JsonNode value) {
        Objects.requireNonNull(value, "value");
        if (!value.isObject() && !value.isArray()) {
            throw new IllegalArgumentException("params must be an object or array JSON node");
        }
        return assignParams(snapshot(value));
    }

    /**
     * Sets array-style {@code params}.
     *
     * @param elements array entries to add; {@code null} entries become JSON nulls
     * @return this builder
     * @throws NullPointerException if {@code elements} is {@code null}
     * @throws IllegalStateException if params have already been configured on this builder
     */
    public JsonRpcRequestBuilder paramsArray(JsonNode... elements) {
        Objects.requireNonNull(elements, "elements");
        ArrayNode arrayNode = NODE_FACTORY.arrayNode();
        for (JsonNode element : elements) {
            arrayNode.add(element == null ? NODE_FACTORY.nullNode() : snapshot(element));
        }
        return assignParams(arrayNode);
    }

    /**
     * Sets object-style {@code params}.
     *
     * @param configurator callback that populates the created object node
     * @return this builder
     * @throws NullPointerException if {@code configurator} is {@code null}
     * @throws IllegalStateException if params have already been configured on this builder
     */
    public JsonRpcRequestBuilder paramsObject(Consumer<ObjectNode> configurator) {
        Objects.requireNonNull(configurator, "configurator");
        ObjectNode objectNode = NODE_FACTORY.objectNode();
        configurator.accept(objectNode);
        return assignParams(objectNode);
    }

    /**
     * Builds an outbound JSON-RPC request or notification payload.
     * <p>
     * A fresh snapshot of the configured {@code params} is written into the returned node so that mutations applied to
     * a built payload do not affect subsequent {@code buildNode()} calls on the same builder.
     * </p>
     *
     * @return transport-ready JSON object node
     * @throws IllegalStateException if this builder represents a request and no id has been configured
     */
    public ObjectNode buildNode() {
        if (!notification && !idConfigured) {
            throw new IllegalStateException("Requests must define an id before buildNode()");
        }

        ObjectNode node = NODE_FACTORY.objectNode();
        node.put("jsonrpc", JsonRpcConstants.VERSION);
        node.put("method", method);
        if (idConfigured) {
            node.set("id", Objects.requireNonNull(id, "id"));
        }
        if (paramsConfigured) {
            node.set("params", snapshot(Objects.requireNonNull(params, "params")));
        }
        return node;
    }

    private JsonRpcRequestBuilder assignId(JsonNode value) {
        if (notification) {
            throw new IllegalStateException("Notifications must not define an id");
        }
        if (idConfigured) {
            throw new IllegalStateException("id has already been configured");
        }
        this.id = value;
        this.idConfigured = true;
        return this;
    }

    private JsonRpcRequestBuilder assignParams(JsonNode value) {
        if (paramsConfigured) {
            throw new IllegalStateException("params have already been configured");
        }
        this.params = value;
        this.paramsConfigured = true;
        return this;
    }

    private static String validateMethod(String method) {
        Objects.requireNonNull(method, "method");
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (method.startsWith(JsonRpcConstants.RESERVED_METHOD_PREFIX)) {
            throw new IllegalArgumentException("method must not use the reserved 'rpc.' namespace");
        }
        return method;
    }

    /**
     * Recursively copies container nodes while reusing immutable scalar nodes.
     * <p>
     * This builder snapshots {@code params} both at assignment time and at build time:
     * </p>
     * <ul>
     *   <li>assignment-time snapshot protects builder state from later mutations to caller-owned input nodes</li>
     *   <li>build-time snapshot protects builder state from later mutations to the returned payload node</li>
     * </ul>
     *
     * @param value node to snapshot
     * @return detached node tree for container values, or the same instance for scalar values
     */
    private static JsonNode snapshot(JsonNode value) {
        if (value.isObject()) {
            ObjectNode copy = NODE_FACTORY.objectNode();
            value.forEachEntry((name, child) -> copy.set(name, snapshot(child)));
            return copy;
        }
        if (value.isArray()) {
            ArrayNode copy = NODE_FACTORY.arrayNode();
            for (JsonNode child : value) {
                copy.add(snapshot(child));
            }
            return copy;
        }
        return value;
    }
}
