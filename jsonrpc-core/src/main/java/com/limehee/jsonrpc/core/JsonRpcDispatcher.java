package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

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
    private final boolean hasInterceptors;
    private final JsonRpcNotificationExecutor notificationExecutor;

    public JsonRpcDispatcher() {
        this(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100,
                List.of(),
                new DirectJsonRpcNotificationExecutor()
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
                List.of(),
                new DirectJsonRpcNotificationExecutor()
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
        this(
                methodRegistry,
                requestParser,
                requestValidator,
                methodInvoker,
                exceptionResolver,
                responseComposer,
                maxBatchSize,
                interceptors,
                new DirectJsonRpcNotificationExecutor()
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
            List<JsonRpcInterceptor> interceptors,
            JsonRpcNotificationExecutor notificationExecutor
    ) {
        this.methodRegistry = methodRegistry;
        this.requestParser = requestParser;
        this.requestValidator = requestValidator;
        this.methodInvoker = methodInvoker;
        this.exceptionResolver = exceptionResolver;
        this.responseComposer = responseComposer;
        this.maxBatchSize = maxBatchSize;
        this.interceptors = List.copyOf(interceptors);
        this.hasInterceptors = !this.interceptors.isEmpty();
        this.notificationExecutor = notificationExecutor;
    }

    public void register(String method, JsonRpcMethodHandler handler) {
        methodRegistry.register(method, handler);
    }

    public JsonRpcDispatchResult dispatch(@Nullable JsonNode payload) {
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

            List<JsonRpcResponse> responses = new ArrayList<>(payload.size());
            for (JsonNode node : payload) {
                dispatchSingleNode(node).ifPresent(responses::add);
            }
            return JsonRpcDispatchResult.batch(responses);
        }

        return JsonRpcDispatchResult.single(dispatchSingleNode(payload));
    }

    public @Nullable JsonRpcResponse dispatch(@Nullable JsonRpcRequest request) {
        boolean validRequest = false;
        try {
            requestValidator.validate(request);
            validRequest = true;
            return dispatchSingleRequest(request).orElse(null);
        } catch (Throwable ex) {
            JsonNode id = request == null ? null : normalizeErrorId(request.id());
            return handleRequestError(id, request, validRequest, ex).orElse(null);
        }
    }

    public JsonRpcResponse parseErrorResponse() {
        return responseComposer.error(null, JsonRpcError.of(
                JsonRpcErrorCode.PARSE_ERROR,
                JsonRpcConstants.MESSAGE_PARSE_ERROR));
    }

    private Optional<JsonRpcResponse> dispatchSingleNode(@Nullable JsonNode node) {
        if (node == null || !node.isObject()) {
            return Optional.of(errorResponse(null, new JsonRpcException(
                    JsonRpcErrorCode.INVALID_REQUEST,
                    JsonRpcConstants.MESSAGE_INVALID_REQUEST)));
        }

        JsonNode errorId = extractIdForError(node);
        JsonRpcRequest request = null;
        boolean validRequest = false;

        try {
            runBeforeValidate(node);
            request = requestParser.parse(node);
            requestValidator.validate(request);
            validRequest = true;
            return dispatchSingleRequest(request);
        } catch (Throwable ex) {
            return handleRequestError(errorId, request, validRequest, ex);
        }
    }

    private Optional<JsonRpcResponse> dispatchSingleRequest(JsonRpcRequest request) throws Exception {
        JsonRpcMethodHandler handler = methodRegistry.find(request.method())
                .orElseThrow(() -> new JsonRpcException(
                        JsonRpcErrorCode.METHOD_NOT_FOUND,
                        JsonRpcConstants.MESSAGE_METHOD_NOT_FOUND));

        if (request.isNotification()) {
            notificationExecutor.execute(() -> invokeNotificationHandler(request, handler));
            return Optional.empty();
        }

        runBeforeInvoke(request);
        JsonNode result = methodInvoker.invoke(handler, request.params());
        runAfterInvoke(request, result);
        return Optional.of(responseComposer.success(request.id(), result));
    }

    private JsonRpcResponse errorResponse(@Nullable JsonNode id, Throwable ex) {
        JsonRpcError error = exceptionResolver.resolve(ex);
        runOnError(null, ex, error);
        return responseComposer.error(id, error);
    }

    private Optional<JsonRpcResponse> handleRequestError(
            @Nullable JsonNode id,
            @Nullable JsonRpcRequest request,
            boolean validRequest,
            Throwable ex
    ) {
        JsonRpcError error = exceptionResolver.resolve(ex);
        runOnError(request, ex, error);

        if (validRequest && request != null && request.isNotification()) {
            return Optional.empty();
        }
        return Optional.of(responseComposer.error(id, error));
    }

    private @Nullable JsonNode extractIdForError(JsonNode node) {
        JsonNode id = node.get("id");
        return normalizeErrorId(id);
    }

    private @Nullable JsonNode normalizeErrorId(@Nullable JsonNode id) {
        if (id == null || id.isNull() || id.isTextual() || id.isNumber()) {
            return id;
        }
        return null;
    }

    private void runBeforeValidate(JsonNode node) {
        if (!hasInterceptors) {
            return;
        }
        for (JsonRpcInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeValidate(node);
            } catch (JsonRpcException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new JsonRpcInterceptorExecutionException(ex);
            }
        }
    }

    private void runBeforeInvoke(JsonRpcRequest request) {
        if (!hasInterceptors) {
            return;
        }
        for (JsonRpcInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeInvoke(request);
            } catch (JsonRpcException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new JsonRpcInterceptorExecutionException(ex);
            }
        }
    }

    private void runAfterInvoke(JsonRpcRequest request, JsonNode result) {
        if (!hasInterceptors) {
            return;
        }
        for (JsonRpcInterceptor interceptor : interceptors) {
            try {
                interceptor.afterInvoke(request, result);
            } catch (JsonRpcException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                throw new JsonRpcInterceptorExecutionException(ex);
            }
        }
    }

    private void runOnError(@Nullable JsonRpcRequest request, Throwable throwable, JsonRpcError error) {
        if (!hasInterceptors) {
            return;
        }
        for (JsonRpcInterceptor interceptor : interceptors) {
            try {
                interceptor.onError(request, throwable, error);
            } catch (Exception ignored) {
            }
        }
    }

    private void invokeNotificationHandler(JsonRpcRequest request, JsonRpcMethodHandler handler) {
        try {
            runBeforeInvoke(request);
            JsonNode result = methodInvoker.invoke(handler, request.params());
            runAfterInvoke(request, result);
        } catch (Throwable ex) {
            JsonRpcError error = exceptionResolver.resolve(ex);
            runOnError(request, ex, error);
        }
    }
}
