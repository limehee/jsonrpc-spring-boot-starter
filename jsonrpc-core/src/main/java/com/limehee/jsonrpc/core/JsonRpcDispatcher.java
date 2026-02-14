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

    public JsonRpcDispatcher() {
        this(
                new InMemoryJsonRpcMethodRegistry(),
                new DefaultJsonRpcRequestParser(),
                new DefaultJsonRpcRequestValidator(),
                new DefaultJsonRpcMethodInvoker(),
                new DefaultJsonRpcExceptionResolver(),
                new DefaultJsonRpcResponseComposer(),
                100
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
        this.methodRegistry = methodRegistry;
        this.requestParser = requestParser;
        this.requestValidator = requestValidator;
        this.methodInvoker = methodInvoker;
        this.exceptionResolver = exceptionResolver;
        this.responseComposer = responseComposer;
        this.maxBatchSize = maxBatchSize;
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
        return dispatchSingleRequest(request).orElse(null);
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

        try {
            JsonRpcRequest request = requestParser.parse(node);
            requestValidator.validate(request);
            return dispatchSingleRequest(request);
        } catch (Throwable ex) {
            return Optional.of(errorResponse(errorId, ex));
        }
    }

    private Optional<JsonRpcResponse> dispatchSingleRequest(JsonRpcRequest request) {
        try {
            JsonRpcMethodHandler handler = methodRegistry.find(request.method())
                    .orElseThrow(() -> new JsonRpcException(
                            JsonRpcErrorCode.METHOD_NOT_FOUND,
                            JsonRpcConstants.MESSAGE_METHOD_NOT_FOUND));

            JsonNode result = methodInvoker.invoke(handler, request.params());
            if (request.isNotification()) {
                return Optional.empty();
            }
            return Optional.of(responseComposer.success(request.id(), result));
        } catch (Throwable ex) {
            if (request.isNotification()) {
                return Optional.empty();
            }
            return Optional.of(errorResponse(request.id(), ex));
        }
    }

    private JsonRpcResponse errorResponse(JsonNode id, Throwable ex) {
        JsonRpcError error = exceptionResolver.resolve(ex);
        return responseComposer.error(id, error);
    }

    private JsonNode extractIdForError(JsonNode node) {
        JsonNode id = node.get("id");
        if (id == null || id.isNull() || id.isTextual() || id.isNumber()) {
            return id;
        }
        return null;
    }
}
