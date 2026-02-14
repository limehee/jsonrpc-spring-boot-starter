package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class JsonRpcDispatcherBenchmark {

    private static final int LARGE_BATCH_SIZE = 64;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonRpcDispatcher dispatcher;
    private JsonNode singlePayload;
    private JsonNode methodNotFoundPayload;
    private JsonNode invalidParamsPayload;
    private JsonNode invalidRequestPayload;
    private JsonNode batchPayload;
    private JsonNode batchAllSuccessLargePayload;
    private JsonNode batchAllErrorsLargePayload;
    private JsonNode batchMixedLargePayload;
    private JsonNode batchNotificationOnlyLargePayload;
    private JsonNode notificationPayload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
        dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));
        dispatcher.register("strict.object", params -> {
            if (params == null || !params.isObject()) {
                throw new JsonRpcException(JsonRpcErrorCode.INVALID_PARAMS, JsonRpcConstants.MESSAGE_INVALID_PARAMS);
            }
            return TextNode.valueOf("ok");
        });

        singlePayload = OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);
        methodNotFoundPayload = OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"missing","id":9}
                """);
        invalidParamsPayload = OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"strict.object","params":"wrong","id":10}
                """);
        invalidRequestPayload = OBJECT_MAPPER.readTree("""
                {"jsonrpc":"1.0","method":"ping","id":11}
                """);
        notificationPayload = OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping"}
                """);
        batchPayload = OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","method":"ping","id":1},
                  {"jsonrpc":"2.0","method":"ping"},
                  {"jsonrpc":"2.0","method":"ping","id":2}
                ]
                """);
        batchAllSuccessLargePayload = buildLargeBatchPayload("ping", LARGE_BATCH_SIZE, false);
        batchAllErrorsLargePayload = buildLargeBatchPayload("missing", LARGE_BATCH_SIZE, false);
        batchMixedLargePayload = buildMixedLargeBatchPayload(LARGE_BATCH_SIZE);
        batchNotificationOnlyLargePayload = buildLargeBatchPayload("ping", LARGE_BATCH_SIZE, true);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchSingle() {
        return dispatcher.dispatch(singlePayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchSingleMethodNotFound() {
        return dispatcher.dispatch(methodNotFoundPayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchSingleInvalidParams() {
        return dispatcher.dispatch(invalidParamsPayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchSingleInvalidRequest() {
        return dispatcher.dispatch(invalidRequestPayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchNotification() {
        return dispatcher.dispatch(notificationPayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchBatchMixed() {
        return dispatcher.dispatch(batchPayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchBatchAllSuccessLarge() {
        return dispatcher.dispatch(batchAllSuccessLargePayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchBatchAllErrorsLarge() {
        return dispatcher.dispatch(batchAllErrorsLargePayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchBatchMixedLarge() {
        return dispatcher.dispatch(batchMixedLargePayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchBatchNotificationOnlyLarge() {
        return dispatcher.dispatch(batchNotificationOnlyLargePayload);
    }

    private JsonNode buildLargeBatchPayload(String method, int size, boolean notificationOnly) {
        ArrayNode array = OBJECT_MAPPER.createArrayNode();
        for (int i = 0; i < size; i++) {
            ObjectNode request = OBJECT_MAPPER.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("method", method);
            if (!notificationOnly) {
                request.put("id", i + 1);
            }
            array.add(request);
        }
        return array;
    }

    private JsonNode buildMixedLargeBatchPayload(int size) {
        ArrayNode array = OBJECT_MAPPER.createArrayNode();
        for (int i = 0; i < size; i++) {
            ObjectNode request = OBJECT_MAPPER.createObjectNode();
            request.put("jsonrpc", "2.0");
            if (i % 3 == 0) {
                request.put("method", "missing");
            } else {
                request.put("method", "ping");
            }
            if (i % 5 != 0) {
                request.put("id", i + 1);
            }
            array.add(request);
        }
        return array;
    }
}
