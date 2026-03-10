package com.limehee.jsonrpc.sample.purejava;

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
            .id("req-7")
            .paramsObject(params -> {
                params.put("sku", "book-001");
                params.put("warehouse", "seoul");
            })
            .buildNode();
    }

    public static ObjectNode buildAuditNotification() {
        return JsonRpcRequestBuilder.notification("audit.record")
            .paramsObject(params -> {
                params.put("event", "inventory.lookup");
                params.put("source", "gateway");
            })
            .buildNode();
    }

    public static ArrayNode buildOutboundBatch() {
        return new JsonRpcRequestBatchBuilder()
            .add(JsonRpcRequestBuilder.request("inventory.lookup").id(1L))
            .addNotification("audit.record", request -> request.paramsObject(params -> {
                params.put("event", "inventory.lookup");
                params.put("source", "gateway");
            }))
            .buildNode();
    }

    public static JsonRpcError buildUpstreamFailureError() {
        ObjectNode data = NODE_FACTORY.objectNode();
        data.put("traceId", "trace-123");
        data.put("service", "inventory-gateway");
        return JsonRpcError.of(-32001, "Inventory upstream failed", data);
    }
}
