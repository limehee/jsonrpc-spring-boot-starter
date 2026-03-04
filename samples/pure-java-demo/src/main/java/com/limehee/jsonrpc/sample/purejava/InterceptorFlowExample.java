package com.limehee.jsonrpc.sample.purejava;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcError;
import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import tools.jackson.databind.node.StringNode;

import java.util.ArrayList;
import java.util.List;

public final class InterceptorFlowExample {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private InterceptorFlowExample() {
    }

    public static Result execute(String rawRequest) throws JacksonException {
        List<String> events = new ArrayList<>();
        JsonRpcInterceptor recording = new JsonRpcInterceptor() {
            @Override
            public void beforeValidate(JsonNode rawRequestNode) {
                events.add("beforeValidate");
            }

            @Override
            public void beforeInvoke(JsonRpcRequest request) {
                events.add("beforeInvoke:" + request.method());
            }

            @Override
            public void afterInvoke(JsonRpcRequest request, JsonNode result) {
                events.add("afterInvoke:" + result.asString());
            }

            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                events.add("onError:" + mappedError.code());
            }
        };
        JsonRpcInterceptor noisyOnError = new JsonRpcInterceptor() {
            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                throw new IllegalStateException("ignored-on-error-failure");
            }
        };

        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            100,
            List.of(recording, noisyOnError)
        );

        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
        dispatcher.register("explode", params -> {
            throw new RuntimeException("boom");
        });

        JsonNode payload = OBJECT_MAPPER.readTree(rawRequest);
        JsonRpcDispatchResult dispatchResult = dispatcher.dispatch(payload);
        return new Result(events, dispatchResult);
    }

    public record Result(List<String> events, JsonRpcDispatchResult dispatchResult) {

    }
}
