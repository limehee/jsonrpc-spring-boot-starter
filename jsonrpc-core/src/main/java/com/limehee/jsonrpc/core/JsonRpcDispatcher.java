package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonRpcDispatcher {

    private final JsonRpcMethodRegistry methodRegistry;
    private final JsonRpcRequestParser requestParser;
    private final JsonRpcRequestValidator requestValidator;
    private final JsonRpcMethodInvoker methodInvoker;
    private final JsonRpcExceptionResolver exceptionResolver;
    private final JsonRpcResponseComposer responseComposer;
    private final int maxBatchSize;
    private final List<JsonRpcInterceptor> interceptors;

    public JsonRpcDispatcher() {
        this(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100,
                List.of()
        );
    }

    public JsonRpcDispatcher(
            JsonRpcMethodRegistry methodRegistry,
            JsonRpcRequestParser requestParser,
            JsonRpcRequestValidator requestValidator,
            JsonRpcMethodInvoker methodInvoker,
            JsonRpcExceptionResolver exceptionResolver,
            JsonRpcResponseComposer responseComposer,
            int maxBatchSize
    ) {
        this(
                methodRegistry,
                requestParser,
                requestValidator,
                methodInvoker,
                exceptionResolver,
                responseComposer,
                maxBatchSize,
                List.of()
        );
    }

    public JsonRpcDispatcher(
            JsonRpcMethodRegistry methodRegistry,
            JsonRpcRequestParser requestParser,
            JsonRpcRequestValidator requestValidator,
            JsonRpcMethodInvoker methodInvoker,
            JsonRpcExceptionResolver exceptionResolver,
            JsonRpcResponseComposer responseComposer,
            int maxBatchSize,
            List<JsonRpcInterceptor> interceptors
    ) {
        this.methodRegistry = methodRegistry;
        this.requestParser = requestParser;
        this.requestValidator = requestValidator;
        this.methodInvoker = methodInvoker;
        this.exceptionResolver = exceptionResolver;
        this.responseComposer = responseComposer;
        this.maxBatchSize = maxBatchSize;
        this.interceptors = List.copyOf(interceptors);
    }

    public void register(String method, JsonRpcMethodHandler handler) {
        methodRegistry.register(method, handler);
    }

    public JsonRpcDispatchResult dispatch(JsonNode payload) {
        if (payload == null) {
            return JsonRpcDispatchResult.single(errorResponse(null, new JsonRpcException(
                    JsonRpcErrorCode.INVALID_REQUEST,
                    JsonRpcConstants.MESSAGE_INVALID_REQUEST)));
        }

        if (payload.isArray()) {
            if (payload.isEmpty()) {
                return JsonRpcDispatchResult.single(errorResponse(null, new JsonRpcException(
                        JsonRpcErrorCode.INVALID_REQUEST,
                        JsonRpcConstants.MESSAGE_INVALID_REQUEST)));
            }
            if (payload.size() > maxBatchSize) {
                return JsonRpcDispatchResult.single(errorResponse(null, new JsonRpcException(
                        JsonRpcErrorCode.INVALID_REQUEST,
                        "Batch size exceeds configured maximum")));
            }

            List<JsonRpcResponse> responses = new ArrayList<>();
            for (JsonNode node : payload) {
                dispatchSingleNode(node).ifPresent(responses::add);
            }
            return JsonRpcDispatchResult.batch(responses);
        }

        return JsonRpcDispatchResult.single(dispatchSingleNode(payload));
    }

    public JsonRpcResponse dispatch(JsonRpcRequest request) {
        try {
            runBeforeInvoke(request);
            return dispatchSingleRequest(request).orElse(null);
        } catch (Throwable ex) {
            if (request != null && request.isNotification()) {
                return null;
            }
            return errorResponse(request == null ? null : request.id(), ex);
        }
    }

    public JsonRpcResponse parseErrorResponse() {
        return responseComposer.error(null, JsonRpcError.of(
                JsonRpcErrorCode.PARSE_ERROR,
                JsonRpcConstants.MESSAGE_PARSE_ERROR));
    }

    private Optional<JsonRpcResponse> dispatchSingleNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Optional.of(errorResponse(null, new JsonRpcException(
                    JsonRpcErrorCode.INVALID_REQUEST,
                    JsonRpcConstants.MESSAGE_INVALID_REQUEST)));
        }

        JsonNode errorId = extractIdForError(node);
        JsonRpcRequest request = null;

        try {
            runBeforeValidate(node);
            request = requestParser.parse(node);
            requestValidator.validate(request);
            runBeforeInvoke(request);
            return dispatchSingleRequest(request);
        } catch (Throwable ex) {
            return handleRequestError(errorId, request, ex);
        }
    }

    private Optional<JsonRpcResponse> dispatchSingleRequest(JsonRpcRequest request) throws Exception {
        JsonRpcMethodHandler handler = methodRegistry.find(request.method())
                .orElseThrow(() -> new JsonRpcException(
                        JsonRpcErrorCode.METHOD_NOT_FOUND,
                        JsonRpcConstants.MESSAGE_METHOD_NOT_FOUND));

        JsonNode result = methodInvoker.invoke(handler, request.params());
        runAfterInvoke(request, result);
        if (request.isNotification()) {
            return Optional.empty();
        }
        return Optional.of(responseComposer.success(request.id(), result));
    }

    private JsonRpcResponse errorResponse(JsonNode id, Throwable ex) {
        JsonRpcError error = exceptionResolver.resolve(ex);
        runOnError(null, ex, error);
        return responseComposer.error(id, error);
    }

    private Optional<JsonRpcResponse> handleRequestError(JsonNode id, JsonRpcRequest request, Throwable ex) {
        JsonRpcError error = exceptionResolver.resolve(ex);
        runOnError(request, ex, error);

        if (request != null && request.isNotification()) {
            return Optional.empty();
        }
        return Optional.of(responseComposer.error(id, error));
    }

    private JsonNode extractIdForError(JsonNode node) {
        JsonNode id = node.get("id");
        if (id == null || id.isNull() || id.isTextual() || id.isNumber()) {
            return id;
        }
        return null;
    }

    private void runBeforeValidate(JsonNode node) {
        interceptors.forEach(interceptor -> interceptor.beforeValidate(node));
    }

    private void runBeforeInvoke(JsonRpcRequest request) {
        interceptors.forEach(interceptor -> interceptor.beforeInvoke(request));
    }

    private void runAfterInvoke(JsonRpcRequest request, JsonNode result) {
        interceptors.forEach(interceptor -> interceptor.afterInvoke(request, result));
    }

    private void runOnError(JsonRpcRequest request, Throwable throwable, JsonRpcError error) {
        for (JsonRpcInterceptor interceptor : interceptors) {
            try {
                interceptor.onError(request, throwable, error);
            } catch (Exception ignored) {
                // Avoid masking JSON-RPC error responses because of interceptor failures.
            }
        }
    }
}
