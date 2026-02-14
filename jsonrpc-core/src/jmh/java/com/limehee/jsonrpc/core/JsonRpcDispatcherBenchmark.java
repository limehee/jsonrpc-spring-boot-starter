package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class JsonRpcDispatcherBenchmark {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonRpcDispatcher dispatcher;
    private JsonNode singlePayload;
    private JsonNode batchPayload;
    private JsonNode notificationPayload;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
        dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));

        singlePayload = OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":1}
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
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchSingle() {
        return dispatcher.dispatch(singlePayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchNotification() {
        return dispatcher.dispatch(notificationPayload);
    }

    @Benchmark
    public JsonRpcDispatchResult dispatchBatchMixed() {
        return dispatcher.dispatch(batchPayload);
    }
}
