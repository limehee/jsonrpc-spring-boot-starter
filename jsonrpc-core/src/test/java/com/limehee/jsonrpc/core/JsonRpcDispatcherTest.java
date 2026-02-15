package com.limehee.jsonrpc.core;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRpcDispatcherTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void dispatchSingleRequestReturnsSuccess() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """));

        assertFalse(result.isBatch());
        assertTrue(result.hasResponse());
        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals("2.0", response.jsonrpc());
        assertEquals(1, response.id().asInt());
        assertEquals("pong", response.result().asText());
    }

    @Test
    void dispatchSingleNotificationReturnsNoResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping"}
                """));

        assertFalse(result.hasResponse());
        assertTrue(result.singleResponse().isEmpty());
    }

    @Test
    void dispatchInvalidRequestWithoutIdReturnsErrorResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","params":[]}
                """));

        assertTrue(result.hasResponse());
        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
        assertNull(response.id());
    }

    @Test
    void dispatchInvalidRequestWithBooleanIdReturnsNullIdInErrorResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":true}
                """));

        assertTrue(result.hasResponse());
        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
        assertNull(response.id());
    }

    @Test
    void dispatchNotificationMethodNotFoundReturnsNoResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"missing"}
                """));

        assertFalse(result.hasResponse());
        assertTrue(result.singleResponse().isEmpty());
    }

    @Test
    void dispatchNotificationUsesNotificationExecutor() throws Exception {
        RecordingNotificationExecutor notificationExecutor = new RecordingNotificationExecutor();
        AtomicInteger invocationCount = new AtomicInteger();
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100,
                List.of(),
                notificationExecutor
        );
        dispatcher.register("ping", params -> {
            invocationCount.incrementAndGet();
            return StringNode.valueOf("pong");
        });

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping"}
                """));

        assertFalse(result.hasResponse());
        assertEquals(1, notificationExecutor.executeCount);
        assertEquals(1, invocationCount.get());
    }

    @Test
    void dispatchRequestWithExplicitNullIdReturnsResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","id":null}
                """));

        assertTrue(result.hasResponse());
        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertTrue(response.id().isNull());
        assertEquals("pong", response.result().asText());
    }

    @Test
    void dispatchMethodNotFoundReturnsError() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"unknown","id":1}
                """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertNotNull(response.error());
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, response.error().code());
    }

    @Test
    void dispatchInvalidParamsReturnsError() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"ping","params":1,"id":1}
                """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, response.error().code());
    }

    @Test
    void dispatchBatchReturnsOnlyNonNotificationResponses() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","method":"ping","id":1},
                  {"jsonrpc":"2.0","method":"ping"},
                  {"jsonrpc":"2.0","method":"missing","id":2},
                  1
                ]
                """));

        assertTrue(result.isBatch());
        List<JsonRpcResponse> responses = result.responses();
        assertEquals(3, responses.size());

        assertEquals(1, responses.get(0).id().asInt());
        assertEquals("pong", responses.get(0).result().asText());

        assertEquals(2, responses.get(1).id().asInt());
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, responses.get(1).error().code());

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, responses.get(2).error().code());
    }

    @Test
    void dispatchBatchIncludesInvalidRequestWithoutId() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","params":[]},
                  {"jsonrpc":"2.0","method":"ping"}
                ]
                """));

        assertTrue(result.isBatch());
        assertEquals(1, result.responses().size());
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, result.responses().get(0).error().code());
        assertNull(result.responses().get(0).id());
    }

    @Test
    void dispatchBatchInvalidIdTypeUsesNullIdInErrorResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","method":"ping","id":{"x":1}},
                  {"jsonrpc":"2.0","method":"ping","id":1}
                ]
                """));

        assertTrue(result.isBatch());
        assertEquals(2, result.responses().size());
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, result.responses().get(0).error().code());
        assertNull(result.responses().get(0).id());
        assertEquals("pong", result.responses().get(1).result().asText());
    }

    @Test
    void dispatchNotificationOnlyBatchReturnsNoResponses() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","method":"ping"},
                  {"jsonrpc":"2.0","method":"ping"}
                ]
                """));

        assertTrue(result.isBatch());
        assertFalse(result.hasResponse());
    }

    @Test
    void dispatchEmptyBatchReturnsSingleInvalidRequest() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("[]"));

        assertFalse(result.isBatch());
        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
    }

    @Test
    void dispatchBatchOverLimitReturnsInvalidRequest() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                1,
                List.of()
        );
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                [
                  {"jsonrpc":"2.0","method":"ping","id":1},
                  {"jsonrpc":"2.0","method":"ping","id":2}
                ]
                """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
    }

    @Test
    void parseErrorResponseReturnsParseErrorCode() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcResponse response = dispatcher.parseErrorResponse();

        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
        assertEquals(JsonRpcConstants.MESSAGE_PARSE_ERROR, response.error().message());
    }

    @Test
    void errorResponseWithUnknownIdSerializesIdAsNull() {
        JsonRpcResponse response = JsonRpcResponse.error(null, JsonRpcErrorCode.INVALID_REQUEST, JsonRpcConstants.MESSAGE_INVALID_REQUEST);
        ObjectNode node = OBJECT_MAPPER.valueToTree(response);

        assertTrue(node.has("id"));
        assertTrue(node.get("id").isNull());
    }

    @Test
    void registeringReservedMethodThrowsByDefault() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        assertThrows(IllegalArgumentException.class,
                () -> dispatcher.register("rpc.system", params -> StringNode.valueOf("ok")));
    }

    @Test
    void legacyDispatchMethodSupportsSingleRequest() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "ping", null, true);
        JsonRpcResponse response = dispatcher.dispatch(request);

        assertNotNull(response);
        assertEquals("pong", response.result().asText());
    }

    @Test
    void legacyDispatchInvalidRequestWithoutIdReturnsInvalidRequestError() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcRequest request = new JsonRpcRequest("1.0", null, "ping", null, false);
        JsonRpcResponse response = dispatcher.dispatch(request);

        assertNotNull(response);
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
        assertNull(response.id());
    }

    @Test
    void legacyDispatchValidNotificationMethodNotFoundReturnsNull() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcRequest request = new JsonRpcRequest("2.0", null, "missing", null, false);
        JsonRpcResponse response = dispatcher.dispatch(request);

        assertNull(response);
    }

    @Test
    void interceptorCallbacksRunForSuccessfulRequest() throws Exception {
        RecordingInterceptor interceptor = new RecordingInterceptor();
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100,
                List.of(interceptor)
        );
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        dispatcher.dispatch(OBJECT_MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"));

        assertEquals(List.of("beforeValidate", "beforeInvoke", "afterInvoke"), interceptor.events);
    }

    @Test
    void interceptorOnErrorRunsForMethodErrors() throws Exception {
        RecordingInterceptor interceptor = new RecordingInterceptor();
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100,
                List.of(interceptor)
        );

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"missing\",\"id\":1}"));

        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, result.singleResponse().orElseThrow().error().code());
        assertTrue(interceptor.events.contains("onError:-32601"));
    }

    @Test
    void notificationInvocationErrorTriggersOnErrorInterceptor() throws Exception {
        RecordingInterceptor interceptor = new RecordingInterceptor();
        RecordingNotificationExecutor notificationExecutor = new RecordingNotificationExecutor();
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100,
                List.of(interceptor),
                notificationExecutor
        );
        dispatcher.register("fail", params -> {
            throw new RuntimeException("boom");
        });

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"fail"}
                """));

        assertFalse(result.hasResponse());
        assertTrue(interceptor.events.contains("onError:-32603"));
    }

    @Test
    void interceptorOnErrorFailureDoesNotMaskOriginalError() throws Exception {
        RecordingInterceptor interceptor = new RecordingInterceptor();
        JsonRpcInterceptor throwingInterceptor = new JsonRpcInterceptor() {
            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                throw new RuntimeException("observer failed");
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
                List.of(throwingInterceptor, interceptor)
        );

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"missing","id":1}
                """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, response.error().code());
        assertTrue(interceptor.events.contains("onError:-32601"));
    }

    private static final class RecordingInterceptor implements JsonRpcInterceptor {
        private final List<String> events = new ArrayList<>();

        @Override
        public void beforeValidate(tools.jackson.databind.JsonNode rawRequest) {
            events.add("beforeValidate");
        }

        @Override
        public void beforeInvoke(JsonRpcRequest request) {
            events.add("beforeInvoke");
        }

        @Override
        public void afterInvoke(JsonRpcRequest request, tools.jackson.databind.JsonNode result) {
            events.add("afterInvoke");
        }

        @Override
        public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
            events.add("onError:" + mappedError.code());
        }
    }

    private static final class RecordingNotificationExecutor implements JsonRpcNotificationExecutor {
        private int executeCount;

        @Override
        public void execute(Runnable task) {
            executeCount++;
            task.run();
        }
    }
}
