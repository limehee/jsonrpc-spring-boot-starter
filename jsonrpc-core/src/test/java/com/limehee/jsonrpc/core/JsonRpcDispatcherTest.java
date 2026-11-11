package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

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
        assertEquals("pong", response.result().asString());
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
        assertEquals("pong", response.result().asString());
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
    void dispatchNonStructuredParamsReturnsInvalidRequest() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","params":1,"id":1}
            """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
        assertEquals(1, response.id().asInt());
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
        assertEquals("pong", responses.get(0).result().asString());

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
        assertEquals("pong", result.responses().get(1).result().asString());
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
        JsonRpcResponse response = JsonRpcResponse.error(null, JsonRpcErrorCode.INVALID_REQUEST,
            JsonRpcConstants.MESSAGE_INVALID_REQUEST);
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
    void dispatchRejectsReservedMethodNamespaceEvenWhenCustomRegistryAllowsIt() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new PermissiveMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            100,
            List.of()
        );
        dispatcher.register("rpc.system", params -> StringNode.valueOf("ok"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"rpc.system","id":1}
            """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
    }

    @Test
    void legacyDispatchMethodSupportsSingleRequest() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "ping", null, true);
        JsonRpcResponse response = dispatcher.dispatch(request);

        assertNotNull(response);
        assertEquals("pong", response.result().asString());
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
    void legacyDispatchNullRequestReturnsInvalidRequestError() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcResponse response = dispatcher.dispatch((JsonRpcRequest) null);

        assertNotNull(response);
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
        assertNull(response.id());
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

        JsonRpcDispatchResult result = dispatcher.dispatch(
            OBJECT_MAPPER.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"missing\",\"id\":1}"));

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

    @Test
    void beforeValidateRuntimeExceptionIsMappedToInternalError() throws Exception {
        JsonRpcInterceptor throwingInterceptor = new JsonRpcInterceptor() {
            @Override
            public void beforeValidate(JsonNode rawRequest) {
                throw new IllegalStateException("beforeValidate boom");
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
            List.of(throwingInterceptor)
        );
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":1}
            """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, response.error().code());
        assertEquals(1, response.id().asInt());
    }

    @Test
    void afterInvokeRuntimeExceptionIsMappedToInternalError() throws Exception {
        JsonRpcInterceptor throwingInterceptor = new JsonRpcInterceptor() {
            @Override
            public void afterInvoke(JsonRpcRequest request, JsonNode result) {
                throw new IllegalStateException("afterInvoke boom");
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
            List.of(throwingInterceptor)
        );
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":2}
            """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, response.error().code());
        assertEquals(2, response.id().asInt());
    }

    @Test
    void dispatchRequestPropagatesErrorFromHandler() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("fatal", params -> {
            throw new AssertionError("fatal");
        });

        assertThrows(AssertionError.class, () -> dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"fatal","id":1}
            """)));
    }

    @Test
    void legacyDispatchRequestPropagatesErrorFromHandler() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("fatal", params -> {
            throw new AssertionError("fatal");
        });

        JsonRpcRequest request = new JsonRpcRequest("2.0", IntNode.valueOf(1), "fatal", null, true);

        assertThrows(AssertionError.class, () -> dispatcher.dispatch(request));
    }

    @Test
    void dispatchNotificationPropagatesErrorFromHandler() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("fatal", params -> {
            throw new AssertionError("fatal");
        });

        assertThrows(AssertionError.class, () -> dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"fatal"}
            """)));
    }

    @Test
    void constructorRejectsNonPositiveMaxBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            0,
            List.of(),
            new DirectJsonRpcNotificationExecutor()
        ));

        assertThrows(IllegalArgumentException.class, () -> new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            -1,
            List.of(),
            new DirectJsonRpcNotificationExecutor()
        ));
    }

    private static final class RecordingInterceptor implements JsonRpcInterceptor {

        private final List<String> events = new ArrayList<>();

        @Override
        public void beforeValidate(JsonNode rawRequest) {
            events.add("beforeValidate");
        }

        @Override
        public void beforeInvoke(JsonRpcRequest request) {
            events.add("beforeInvoke");
        }

        @Override
        public void afterInvoke(JsonRpcRequest request, JsonNode result) {
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

    private static final class PermissiveMethodRegistry implements JsonRpcMethodRegistry {

        private final Map<String, JsonRpcMethodHandler> handlers = new HashMap<>();

        @Override
        public void register(String method, JsonRpcMethodHandler handler) {
            handlers.put(method, handler);
        }

        @Override
        public Optional<JsonRpcMethodHandler> find(String method) {
            return Optional.ofNullable(handlers.get(method));
        }
    }

    @Test
    void dispatchNormalizesJavaNullHandlerResultToJsonNullResult() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("noop", params -> null);

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"noop","id":1}
            """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertNull(response.error());
        assertNotNull(response.result());
        assertTrue(response.result().isNull());
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":null}", OBJECT_MAPPER.writeValueAsString(response));
    }

    @Test
    void dispatchNormalizesJavaNullHandlerResultInsideBatch() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("noop", params -> null);

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            [{"jsonrpc":"2.0","method":"noop","id":1},{"jsonrpc":"2.0","method":"noop","id":2}]
            """));

        assertEquals(2, result.responses().size());
        for (JsonRpcResponse response : result.responses()) {
            assertNull(response.error());
            assertTrue(response.result().isNull());
        }
    }

    @Test
    void dispatchPreParsedRequestNormalizesJavaNullHandlerResult() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("noop", params -> null);

        JsonRpcResponse response = dispatcher.dispatch(
            new JsonRpcRequest("2.0", IntNode.valueOf(3), "noop", null, true));

        assertNotNull(response);
        assertNull(response.error());
        assertTrue(response.result().isNull());
    }

    @Test
    void nullHandlerResultIsPassedToInterceptorsAsJsonNullWithoutTriggeringOnError() throws Exception {
        List<JsonNode> afterInvokeResults = new ArrayList<>();
        AtomicInteger onErrorCalls = new AtomicInteger();
        JsonRpcInterceptor interceptor = new JsonRpcInterceptor() {
            @Override
            public void afterInvoke(JsonRpcRequest request, JsonNode result) {
                afterInvokeResults.add(result);
            }

            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                onErrorCalls.incrementAndGet();
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
            List.of(interceptor)
        );
        dispatcher.register("noop", params -> null);

        dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"noop","id":1}
            """));
        dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"noop"}
            """));

        assertEquals(2, afterInvokeResults.size());
        assertTrue(afterInvokeResults.stream().allMatch(JsonNode::isNull));
        assertEquals(0, onErrorCalls.get());
    }

    @Test
    void afterInvokeIsNotCalledWhenSuccessResponseCompositionFails() throws Exception {
        AtomicInteger afterInvokeCalls = new AtomicInteger();
        AtomicInteger onErrorCalls = new AtomicInteger();
        JsonRpcInterceptor interceptor = new JsonRpcInterceptor() {
            @Override
            public void afterInvoke(JsonRpcRequest request, JsonNode result) {
                afterInvokeCalls.incrementAndGet();
            }

            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                onErrorCalls.incrementAndGet();
            }
        };
        JsonRpcResponseComposer failingComposer = new JsonRpcResponseComposer() {
            @Override
            public JsonRpcResponse success(JsonNode id, JsonNode result) {
                throw new IllegalStateException("compose failed");
            }

            @Override
            public JsonRpcResponse error(JsonNode id, JsonRpcError error) {
                return JsonRpcResponse.error(id, error);
            }
        };
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            failingComposer,
            100,
            List.of(interceptor)
        );
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":1}
            """));

        assertEquals(JsonRpcErrorCode.INTERNAL_ERROR, result.singleResponse().orElseThrow().error().code());
        assertEquals(0, afterInvokeCalls.get());
        assertEquals(1, onErrorCalls.get());
    }

    @Test
    void doesNotReplyToNotificationWithReservedMethodName() throws Exception {
        List<JsonRpcError> errors = new ArrayList<>();
        JsonRpcDispatcher dispatcher = dispatcherWithInterceptor(errorRecorder(errors));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"rpc.discover"}
            """));

        assertFalse(result.hasResponse());
        assertEquals(1, errors.size());
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, errors.get(0).code());
    }

    @Test
    void doesNotReplyToNotificationWithEmptyMethodName() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":""}
            """));

        assertFalse(result.hasResponse());
    }

    @Test
    void stillRejectsReservedMethodNameForRequestsWithId() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcResponse response = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"rpc.discover","id":7}
            """)).singleResponse().orElseThrow();

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
        assertEquals(7, response.id().asInt());
    }

    @Test
    void omitsRejectedNotificationsFromBatchResponse() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            [
              {"jsonrpc":"2.0","method":"rpc.x"},
              {"jsonrpc":"2.0","method":""},
              {"jsonrpc":"2.0","method":"ping","id":1}
            ]
            """));

        assertTrue(result.isBatch());
        assertEquals(1, result.responses().size());
        assertEquals("pong", result.responses().get(0).result().asString());
    }

    @Test
    void omitsBatchResponseEntirelyWhenOnlyRejectedNotificationsArePresent() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            [{"jsonrpc":"2.0","method":"rpc.x"},{"jsonrpc":"2.0","method":""}]
            """));

        assertFalse(result.hasResponse());
    }

    @Test
    void doesNotReplyToPreParsedNotificationWithReservedMethodName() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        assertNull(dispatcher.dispatch(new JsonRpcRequest("2.0", null, "rpc.x", null, false)));
    }

    @Test
    void doesNotReplyWhenBeforeValidateInterceptorRejectsValidNotification() throws Exception {
        List<JsonRpcError> errors = new ArrayList<>();
        JsonRpcInterceptor rejecting = new JsonRpcInterceptor() {
            @Override
            public void beforeValidate(JsonNode rawRequest) {
                throw new JsonRpcException(-32001, "Unauthorized");
            }

            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                errors.add(mappedError);
            }
        };
        JsonRpcDispatcher dispatcher = dispatcherWithInterceptor(rejecting);
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

        JsonRpcDispatchResult notification = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping"}
            """));
        JsonRpcDispatchResult request = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"ping","id":1}
            """));

        assertFalse(notification.hasResponse());
        assertEquals(-32001, request.singleResponse().orElseThrow().error().code());
        assertEquals(2, errors.size());
    }

    @Test
    void stillRepliesToStructurallyInvalidNotification() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        JsonRpcDispatchResult wrongVersion = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"1.0","method":"ping"}
            """));
        JsonRpcDispatchResult nonStringMethod = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":1,"params":"bar"}
            """));

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, wrongVersion.singleResponse().orElseThrow().error().code());
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, nonStringMethod.singleResponse().orElseThrow().error().code());
    }

    @Test
    void stillRepliesToNotificationWhenIdMemberIsRequired() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(JsonRpcRequestValidationOptions.builder().requireIdMember(true).build()),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            100
        );

        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
            {"jsonrpc":"2.0","method":"rpc.x"}
            """));

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, result.singleResponse().orElseThrow().error().code());
    }

    private static JsonRpcDispatcher dispatcherWithInterceptor(JsonRpcInterceptor interceptor) {
        return new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            100,
            List.of(interceptor)
        );
    }

    private static JsonRpcInterceptor errorRecorder(List<JsonRpcError> errors) {
        return new JsonRpcInterceptor() {
            @Override
            public void onError(JsonRpcRequest request, Throwable throwable, JsonRpcError mappedError) {
                errors.add(mappedError);
            }
        };
    }

    @Test
    void typedNoParamsHandlerAcceptsExplicitNullParamsWhenAllowed() throws Exception {
        JsonRpcTypedMethodHandlerFactory factory = new DefaultJsonRpcTypedMethodHandlerFactory(
            new JacksonJsonRpcParameterBinder(OBJECT_MAPPER), new JacksonJsonRpcResultWriter(OBJECT_MAPPER));
        JsonRpcDispatcher strict = new JsonRpcDispatcher();
        JsonRpcDispatcher lenient = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(JsonRpcRequestValidationOptions.builder().allowNullParams(true).build()),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(),
            new DefaultJsonRpcResponseComposer(),
            100
        );
        strict.register("ping", factory.noParams(() -> "pong"));
        lenient.register("ping", factory.noParams(() -> "pong"));
        String payload = """
            {"jsonrpc":"2.0","method":"ping","params":null,"id":1}
            """;

        JsonRpcResponse strictResponse = strict.dispatch(OBJECT_MAPPER.readTree(payload)).singleResponse().orElseThrow();
        JsonRpcResponse lenientResponse = lenient.dispatch(OBJECT_MAPPER.readTree(payload)).singleResponse().orElseThrow();

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, strictResponse.error().code());
        assertEquals("pong", lenientResponse.result().asString());
    }
}
