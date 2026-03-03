package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Central JSON-RPC dispatcher orchestrating parsing, validation, invocation, interception, and response composition.
 * <p>
 * The dispatcher supports:
 * <ul>
 *   <li>single request and batch request payloads</li>
 *   <li>notifications (no response body)</li>
 *   <li>interceptor hooks across validation/invocation/error phases</li>
 *   <li>pluggable strategy components for each pipeline stage</li>
 * </ul>
 */
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

    /**
     * Creates a dispatcher with default in-memory registry and default strategy components.
     * <p>
     * Defaults include:
     * <ul>
     *   <li>max batch size: {@code 100}</li>
     *   <li>no interceptors</li>
     *   <li>synchronous notification execution</li>
     * </ul>
     */
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

    /**
     * Creates a dispatcher with custom core pipeline components and default interceptor/notification behavior.
     *
     * @param methodRegistry method registry
     * @param requestParser request parser
     * @param requestValidator request validator
     * @param methodInvoker method invoker
     * @param exceptionResolver exception resolver
     * @param responseComposer response composer
     * @param maxBatchSize maximum number of elements allowed in batch payloads
     */
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

    /**
     * Creates a dispatcher with custom components and interceptors.
     *
     * @param methodRegistry method registry
     * @param requestParser request parser
     * @param requestValidator request validator
     * @param methodInvoker method invoker
     * @param exceptionResolver exception resolver
     * @param responseComposer response composer
     * @param maxBatchSize maximum number of elements allowed in batch payloads
     * @param interceptors interceptor chain executed around request handling
     */
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

    /**
     * Creates a fully customized dispatcher.
     *
     * @param methodRegistry method registry
     * @param requestParser request parser
     * @param requestValidator request validator
     * @param methodInvoker method invoker
     * @param exceptionResolver exception resolver
     * @param responseComposer response composer
     * @param maxBatchSize maximum number of elements allowed in batch payloads
     * @param interceptors interceptor chain executed around request handling
     * @param notificationExecutor executor used for notification invocations
     */
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
        this.methodRegistry = Objects.requireNonNull(methodRegistry, "methodRegistry");
        this.requestParser = Objects.requireNonNull(requestParser, "requestParser");
        this.requestValidator = Objects.requireNonNull(requestValidator, "requestValidator");
        this.methodInvoker = Objects.requireNonNull(methodInvoker, "methodInvoker");
        this.exceptionResolver = Objects.requireNonNull(exceptionResolver, "exceptionResolver");
        this.responseComposer = Objects.requireNonNull(responseComposer, "responseComposer");
        this.maxBatchSize = maxBatchSize;
        this.interceptors = List.copyOf(Objects.requireNonNull(interceptors, "interceptors"));
        this.hasInterceptors = !this.interceptors.isEmpty();
        this.notificationExecutor = Objects.requireNonNull(notificationExecutor, "notificationExecutor");
    }

    /**
     * Registers a method handler.
     *
     * @param method JSON-RPC method name
     * @param handler method handler
     */
    public void register(String method, JsonRpcMethodHandler handler) {
        methodRegistry.register(method, handler);
    }

    /**
     * Dispatches a raw JSON payload.
     *
     * @param payload single request object or batch array
     * @return dispatch result containing zero, one, or many responses depending on payload shape and notification usage
     */
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

    /**
     * Dispatches a pre-parsed request model.
     *
     * @param request parsed request model; may be {@code null}
     * @return response for requests with ids, or {@code null} for notifications/no-response outcomes
     */
    public @Nullable JsonRpcResponse dispatch(@Nullable JsonRpcRequest request) {
        boolean validRequest = false;
        try {
            requestValidator.validate(request);
            validRequest = true;
            return dispatchSingleRequest(request).orElse(null);
        } catch (Error error) {
            throw error;
        } catch (Throwable ex) {
            JsonNode id = request == null ? null : normalizeErrorId(request.id());
            return handleRequestError(id, request, validRequest, ex).orElse(null);
        }
    }

    /**
     * Creates a standard parse-error response.
     *
     * @return parse error response with {@code id = null}
     */
    public JsonRpcResponse parseErrorResponse() {
        return responseComposer.error(null, JsonRpcError.of(
                JsonRpcErrorCode.PARSE_ERROR,
                JsonRpcConstants.MESSAGE_PARSE_ERROR));
    }

    /**
     * Dispatches a single object node from either single-request or batch payload processing.
     *
     * @param node request object node
     * @return optional response; empty for notifications
     */
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
        } catch (Error error) {
            throw error;
        } catch (Throwable ex) {
            return handleRequestError(errorId, request, validRequest, ex);
        }
    }

    /**
     * Invokes a validated request against the method registry.
     *
     * @param request validated request
     * @return optional response; empty for notifications
     * @throws Exception when invocation fails before error mapping
     */
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

    /**
     * Maps an exception to an error response and notifies interceptors.
     *
     * @param id response id
     * @param ex thrown exception
     * @return error response
     */
    private JsonRpcResponse errorResponse(@Nullable JsonNode id, Throwable ex) {
        JsonRpcError error = exceptionResolver.resolve(ex);
        runOnError(null, ex, error);
        return responseComposer.error(id, error);
    }

    /**
     * Handles errors for requests parsed from payload nodes.
     *
     * @param id normalized error id
     * @param request request if parsing/validation reached request construction
     * @param validRequest whether request validation succeeded before error
     * @param ex thrown exception
     * @return response unless the request is a valid notification
     */
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

    /**
     * Extracts and normalizes id from a raw request node for error responses.
     *
     * @param node request object node
     * @return normalized id for error payloads
     */
    private @Nullable JsonNode extractIdForError(JsonNode node) {
        JsonNode id = node.get("id");
        return normalizeErrorId(id);
    }

    /**
     * Normalizes id values for error responses.
     * <p>
     * JSON-RPC allows string/number/null ids. Invalid id types are represented as {@code null} in errors.
     *
     * @param id raw id node
     * @return normalized id or {@code null}
     */
    private @Nullable JsonNode normalizeErrorId(@Nullable JsonNode id) {
        if (id == null || id.isNull() || id.isString() || id.isNumber()) {
            return id;
        }
        return null;
    }

    /**
     * Runs {@code beforeValidate} interceptors.
     *
     * @param node raw request node
     */
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

    /**
     * Runs {@code beforeInvoke} interceptors.
     *
     * @param request validated request
     */
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

    /**
     * Runs {@code afterInvoke} interceptors.
     *
     * @param request validated request
     * @param result invocation result
     */
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

    /**
     * Runs {@code onError} interceptors.
     * <p>
     * Interceptor failures are intentionally ignored to avoid masking original request-processing errors.
     *
     * @param request request model when available
     * @param throwable original throwable
     * @param error mapped JSON-RPC error
     */
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

    /**
     * Executes notification invocation flow.
     *
     * @param request validated notification request
     * @param handler target method handler
     */
    private void invokeNotificationHandler(JsonRpcRequest request, JsonRpcMethodHandler handler) {
        try {
            runBeforeInvoke(request);
            JsonNode result = methodInvoker.invoke(handler, request.params());
            runAfterInvoke(request, result);
        } catch (Error error) {
            throw error;
        } catch (Throwable ex) {
            JsonRpcError error = exceptionResolver.resolve(ex);
            runOnError(request, ex, error);
        }
    }
}
