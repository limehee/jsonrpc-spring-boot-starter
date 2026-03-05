package com.limehee.jsonrpc.spring.webmvc;

import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcPayloadReader;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * HTTP endpoint that exposes JSON-RPC 2.0 over Spring WebMVC.
 * <p>
 * The endpoint accepts {@code application/json} POST payloads, delegates request processing to
 * {@link JsonRpcDispatcher}, and serializes protocol-compliant JSON-RPC response payloads. Notification-only requests
 * return an HTTP response without a body.
 * </p>
 */
@RestController
public class JsonRpcWebMvcEndpoint {

    private final JsonRpcDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final JsonRpcPayloadReader requestPayloadReader;
    private final JsonRpcHttpStatusStrategy httpStatusStrategy;
    private final int maxRequestBytes;
    private final JsonRpcWebMvcObserver observer;

    /**
     * Creates an endpoint with a no-op observer.
     *
     * @param dispatcher         dispatcher that performs JSON-RPC parsing, validation, and invocation
     * @param objectMapper       mapper used to parse request payloads and serialize responses
     * @param httpStatusStrategy strategy that maps JSON-RPC outcomes to HTTP status codes
     * @param maxRequestBytes    maximum accepted request payload size in bytes
     * @throws IllegalArgumentException if {@code maxRequestBytes <= 0}
     */
    public JsonRpcWebMvcEndpoint(
        JsonRpcDispatcher dispatcher,
        ObjectMapper objectMapper,
        JsonRpcHttpStatusStrategy httpStatusStrategy,
        int maxRequestBytes
    ) {
        this(
            dispatcher,
            objectMapper,
            httpStatusStrategy,
            maxRequestBytes,
            JsonRpcWebMvcObserver.noOp(),
            false
        );
    }

    /**
     * Creates an endpoint with an explicit transport observer.
     *
     * @param dispatcher         dispatcher that performs JSON-RPC parsing, validation, and invocation
     * @param objectMapper       mapper used to parse request payloads and serialize responses
     * @param httpStatusStrategy strategy that maps JSON-RPC outcomes to HTTP status codes
     * @param maxRequestBytes    maximum accepted request payload size in bytes
     * @param observer           observer receiving transport-level event callbacks
     * @throws IllegalArgumentException if {@code maxRequestBytes <= 0}
     */
    public JsonRpcWebMvcEndpoint(
        JsonRpcDispatcher dispatcher,
        ObjectMapper objectMapper,
        JsonRpcHttpStatusStrategy httpStatusStrategy,
        int maxRequestBytes,
        JsonRpcWebMvcObserver observer
    ) {
        this(
            dispatcher,
            objectMapper,
            httpStatusStrategy,
            maxRequestBytes,
            observer,
            false
        );
    }

    /**
     * Creates an endpoint with explicit transport observer and request duplicate-member policy.
     *
     * @param dispatcher               dispatcher that performs JSON-RPC parsing, validation, and invocation
     * @param objectMapper             mapper used to parse request payloads and serialize responses
     * @param httpStatusStrategy       strategy that maps JSON-RPC outcomes to HTTP status codes
     * @param maxRequestBytes          maximum accepted request payload size in bytes
     * @param observer                 observer receiving transport-level event callbacks
     * @param rejectDuplicateMembers   {@code true} to reject duplicate request members during JSON parsing
     * @throws IllegalArgumentException if {@code maxRequestBytes <= 0}
     */
    public JsonRpcWebMvcEndpoint(
        JsonRpcDispatcher dispatcher,
        ObjectMapper objectMapper,
        JsonRpcHttpStatusStrategy httpStatusStrategy,
        int maxRequestBytes,
        JsonRpcWebMvcObserver observer,
        boolean rejectDuplicateMembers
    ) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        if (maxRequestBytes <= 0) {
            throw new IllegalArgumentException("maxRequestBytes must be greater than 0");
        }
        this.requestPayloadReader = new JsonRpcPayloadReader(objectMapper, rejectDuplicateMembers);
        this.httpStatusStrategy = Objects.requireNonNull(httpStatusStrategy, "httpStatusStrategy");
        this.maxRequestBytes = maxRequestBytes;
        this.observer = Objects.requireNonNull(observer, "observer");
    }

    /**
     * Handles JSON-RPC HTTP requests.
     * <p>
     * Parsing errors, oversized payloads, and whitespace-only payloads produce a single JSON-RPC error response.
     * Notification-only handling returns an empty HTTP response with a transport status from
     * {@link JsonRpcHttpStatusStrategy#statusForNotificationOnly()}.
     * </p>
     *
     * @param body raw HTTP request payload bytes; may be {@code null} when request body is absent
     * @return HTTP response entity containing either serialized JSON-RPC payload or empty body
     */
    @PostMapping(
        value = "${jsonrpc.path:/jsonrpc}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> invoke(@RequestBody(required = false) byte[] body) {
        if (body == null || body.length == 0) {
            observer.onParseError();
            return singleErrorResponse(dispatcher.parseErrorResponse(), httpStatusStrategy.statusForParseError());
        }
        if (body.length > maxRequestBytes) {
            observer.onRequestTooLarge(body.length, maxRequestBytes);
            JsonRpcResponse response = JsonRpcResponse.error(
                null,
                JsonRpcErrorCode.INVALID_REQUEST,
                "Request payload too large");
            return singleErrorResponse(response, httpStatusStrategy.statusForRequestTooLarge());
        }
        if (isJsonWhitespaceOnly(body)) {
            observer.onParseError();
            return singleErrorResponse(dispatcher.parseErrorResponse(), httpStatusStrategy.statusForParseError());
        }

        JsonNode payload;
        try {
            payload = requestPayloadReader.readTree(body);
        } catch (JacksonException ex) {
            observer.onParseError();
            return singleErrorResponse(dispatcher.parseErrorResponse(), httpStatusStrategy.statusForParseError());
        }
        if (payload == null) {
            observer.onParseError();
            return singleErrorResponse(dispatcher.parseErrorResponse(), httpStatusStrategy.statusForParseError());
        }

        JsonRpcDispatchResult result = dispatcher.dispatch(payload);
        if (!result.hasResponse()) {
            observer.onNotificationOnly(payload.isArray(), payload.isArray() ? payload.size() : 1);
            return ResponseEntity.status(httpStatusStrategy.statusForNotificationOnly()).build();
        }

        if (result.isBatch()) {
            List<JsonRpcResponse> responses = result.responses();
            observer.onBatchResponse(payload.size(), responses);
            return jsonResponse(httpStatusStrategy.statusForBatch(responses), responses);
        }

        JsonRpcResponse single = result.singleResponse().orElseThrow();
        observer.onSingleResponse(single);
        return jsonResponse(httpStatusStrategy.statusForSingle(single), single);
    }

    /**
     * Creates a single-error transport response.
     *
     * @param response JSON-RPC error response payload
     * @param status   HTTP status selected for that payload
     * @return HTTP response entity containing serialized JSON-RPC error payload
     */
    private ResponseEntity<String> singleErrorResponse(JsonRpcResponse response, HttpStatus status) {
        return jsonResponse(status, response);
    }

    /**
     * Serializes the given payload and builds an HTTP response entity.
     *
     * @param status  HTTP status to apply
     * @param payload payload object to serialize as JSON
     * @return HTTP response with JSON content type and serialized body
     */
    private ResponseEntity<String> jsonResponse(HttpStatus status, Object payload) {
        return ResponseEntity
            .status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(toJson(payload));
    }

    /**
     * Serializes an object into JSON text.
     *
     * @param payload payload object to serialize
     * @return serialized JSON text
     * @throws IllegalStateException if serialization fails unexpectedly
     */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize JSON-RPC response payload", ex);
        }
    }

    /**
     * Checks whether the payload consists only of JSON whitespace characters.
     *
     * @param body payload bytes to inspect
     * @return {@code true} when payload is non-empty but whitespace-only
     */
    private boolean isJsonWhitespaceOnly(byte[] body) {
        for (byte value : body) {
            if (value != ' ' && value != '\t' && value != '\n' && value != '\r') {
                return false;
            }
        }
        return true;
    }
}
