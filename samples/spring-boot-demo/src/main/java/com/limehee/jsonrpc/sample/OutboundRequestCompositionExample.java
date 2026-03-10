package com.limehee.jsonrpc.sample;

import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcRequestBatchBuilder;
import com.limehee.jsonrpc.core.JsonRpcRequestBuilder;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

public final class OutboundRequestCompositionExample {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    private OutboundRequestCompositionExample() {
    }

    public static ObjectNode buildInventoryLookupRequest() {
        return JsonRpcRequestBuilder.request("inventory.lookup")
            .id("req-21")
            .paramsObject(params -> {
                params.put("sku", "book-001");
                params.put("warehouse", "spring");
            })
            .buildNode();
    }

    public static ArrayNode buildInventoryAuditBatch() {
        return new JsonRpcRequestBatchBuilder()
            .add(JsonRpcRequestBuilder.request("inventory.lookup").id(21L))
            .addNotification("audit.record", request -> request.paramsObject(params -> {
                params.put("event", "inventory.lookup");
                params.put("source", "spring-boot-demo");
            }))
            .buildNode();
    }

    public static JsonRpcError buildUpstreamFailureError() {
        ObjectNode data = NODE_FACTORY.objectNode();
        data.put("traceId", "trace-spring-123");
        data.put("service", "inventory-gateway");
        return JsonRpcError.of(-32001, "Inventory upstream failed", data);
    }
}
