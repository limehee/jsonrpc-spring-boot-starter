package com.limehee.jsonrpc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Builder for composing outbound JSON-RPC batch request payloads.
 */
public final class JsonRpcRequestBatchBuilder {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    private final List<JsonRpcRequestBuilder> requests = new ArrayList<>();

    /**
     * Adds a preconfigured request or notification builder to the batch.
     *
     * @param builder request builder to add
     * @return this builder
     * @throws NullPointerException if {@code builder} is {@code null}
     */
    public JsonRpcRequestBatchBuilder add(JsonRpcRequestBuilder builder) {
        requests.add(Objects.requireNonNull(builder, "builder"));
        return this;
    }

    /**
     * Creates, customizes, and adds a request builder.
     *
     * @param method     JSON-RPC method name
     * @param customizer callback used to configure the request builder
     * @return this builder
     * @throws NullPointerException     if {@code method} or {@code customizer} is {@code null}
     * @throws IllegalArgumentException if {@code method} is invalid
     */
    public JsonRpcRequestBatchBuilder addRequest(String method, Consumer<JsonRpcRequestBuilder> customizer) {
        Objects.requireNonNull(customizer, "customizer");
        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.request(method);
        customizer.accept(builder);
        return add(builder);
    }

    /**
     * Creates, customizes, and adds a notification builder.
     *
     * @param method     JSON-RPC method name
     * @param customizer callback used to configure the notification builder
     * @return this builder
     * @throws NullPointerException     if {@code method} or {@code customizer} is {@code null}
     * @throws IllegalArgumentException if {@code method} is invalid
     */
    public JsonRpcRequestBatchBuilder addNotification(String method, Consumer<JsonRpcRequestBuilder> customizer) {
        Objects.requireNonNull(customizer, "customizer");
        JsonRpcRequestBuilder builder = JsonRpcRequestBuilder.notification(method);
        customizer.accept(builder);
        return add(builder);
    }

    /**
     * Builds a batch payload.
     *
     * @return transport-ready JSON array node
     * @throws IllegalStateException if no batch entries have been added or if an included request builder is in an
     *                               invalid state when the batch is materialized
     */
    public ArrayNode buildNode() {
        if (requests.isEmpty()) {
            throw new IllegalStateException("Batch requests must contain at least one entry");
        }

        ArrayNode arrayNode = NODE_FACTORY.arrayNode();
        for (JsonRpcRequestBuilder request : requests) {
            arrayNode.add(request.buildNode());
        }
        return arrayNode;
    }
}
