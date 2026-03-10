package com.limehee.jsonrpc.sample;

import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcRequestBatchBuilder;
import com.limehee.jsonrpc.core.JsonRpcRequestBuilder;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Sample outbound JSON-RPC payload composition for a Spring Boot application that also acts as a client.
 * <p>
 * The Spring Boot sample primarily demonstrates server-side request handling through
 * {@code jsonrpc-spring-boot-starter}, but real applications often need to call another JSON-RPC service as part of
 * the same request flow. This example exists to show that outbound payload composition stays in
 * {@code jsonrpc-core} and can be used directly from a Spring application without any transport-specific helper.
 * </p>
 */
public final class OutboundRequestCompositionExample {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    private OutboundRequestCompositionExample() {
    }

    /**
     * Builds a request with object-style params using direct node composition.
     *
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "method": "inventory.lookup",
     *   "id": "req-21",
     *   "params": {
     *     "sku": "book-001",
     *     "warehouse": "spring"
     *   }
     * }
     * }</pre>
     *
     * @return outbound request payload
     */
    public static ObjectNode buildInventoryLookupRequest() {
        return JsonRpcRequestBuilder.request("inventory.lookup")
            .id("req-21")
            .paramsObject(params -> {
                params.put("sku", "book-001");
                params.put("warehouse", "spring");
            })
            .buildNode();
    }

    /**
     * Builds a request from a record converted through Jackson.
     *
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "method": "inventory.lookup.record",
     *   "id": 22,
     *   "params": {
     *     "sku": "book-002",
     *     "warehouse": "busan"
     *   }
     * }
     * }</pre>
     *
     * @return outbound request payload
     */
    public static ObjectNode buildInventoryLookupRequestFromRecord() {
        return JsonRpcRequestBuilder.request("inventory.lookup.record")
            .id(22L)
            .params(OBJECT_MAPPER.valueToTree(new InventoryLookupParams("book-002", "busan")))
            .buildNode();
    }

    /**
     * Builds a request from a classic Java class converted through Jackson.
     *
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "method": "tag.create",
     *   "id": 23,
     *   "params": {
     *     "name": "featured",
     *     "color": "green"
     *   }
     * }
     * }</pre>
     *
     * @return outbound request payload
     */
    public static ObjectNode buildTagCreateRequestFromClass() {
        return JsonRpcRequestBuilder.request("tag.create")
            .id(23L)
            .params(OBJECT_MAPPER.valueToTree(new TagCreateParams("featured", "green")))
            .buildNode();
    }

    /**
     * Builds a request whose params are a JSON array produced from a collection.
     *
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "method": "tags.bulkLookup",
     *   "id": 24,
     *   "params": ["alpha", "beta", "gamma"]
     * }
     * }</pre>
     *
     * @return outbound request payload
     */
    public static ObjectNode buildBulkLookupRequestFromCollection() {
        return JsonRpcRequestBuilder.request("tags.bulkLookup")
            .id(24L)
            .params(OBJECT_MAPPER.valueToTree(List.of("alpha", "beta", "gamma")))
            .buildNode();
    }

    /**
     * Builds a request from a map converted through Jackson.
     *
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "method": "health.snapshot",
     *   "id": 25,
     *   "params": {
     *     "region": "ap-northeast-2",
     *     "includeDetails": true
     *   }
     * }
     * }</pre>
     *
     * @return outbound request payload
     */
    public static ObjectNode buildHealthSnapshotRequestFromMap() {
        return JsonRpcRequestBuilder.request("health.snapshot")
            .id(25L)
            .params(OBJECT_MAPPER.valueToTree(Map.of(
                "region", "ap-northeast-2",
                "includeDetails", true
            )))
            .buildNode();
    }

    /**
     * Builds a mixed batch containing a request and a notification.
     *
     * <pre>{@code
     * [
     *   {
     *     "jsonrpc": "2.0",
     *     "method": "inventory.lookup",
     *     "id": 21
     *   },
     *   {
     *     "jsonrpc": "2.0",
     *     "method": "audit.record",
     *     "params": {
     *       "event": "inventory.lookup",
     *       "source": "spring-boot-demo"
     *     }
     *   }
     * ]
     * }</pre>
     *
     * @return outbound batch payload
     */
    public static ArrayNode buildInventoryAuditBatch() {
        return new JsonRpcRequestBatchBuilder()
            .add(JsonRpcRequestBuilder.request("inventory.lookup").id(21L))
            .addNotification("audit.record", request -> request.paramsObject(params -> {
                params.put("event", "inventory.lookup");
                params.put("source", "spring-boot-demo");
            }))
            .buildNode();
    }

    /**
     * Builds a structured JSON-RPC error object that can be embedded into a response payload.
     *
     * <pre>{@code
     * {
     *   "code": -32001,
     *   "message": "Inventory upstream failed",
     *   "data": {
     *     "traceId": "trace-spring-123",
     *     "service": "inventory-gateway"
     *   }
     * }
     * }</pre>
     *
     * @return JSON-RPC error object
     */
    public static JsonRpcError buildUpstreamFailureError() {
        ObjectNode data = NODE_FACTORY.objectNode();
        data.put("traceId", "trace-spring-123");
        data.put("service", "inventory-gateway");
        return JsonRpcError.of(-32001, "Inventory upstream failed", data);
    }

    public record InventoryLookupParams(String sku, String warehouse) {
    }

    public static final class TagCreateParams {

        private final String name;
        private final String color;

        public TagCreateParams(String name, String color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public String getColor() {
            return color;
        }
    }
}
