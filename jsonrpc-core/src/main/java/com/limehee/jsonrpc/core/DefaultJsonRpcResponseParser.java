package com.limehee.jsonrpc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Default parser for incoming JSON-RPC response payloads.
 */
public class DefaultJsonRpcResponseParser implements JsonRpcResponseParser {

    private final JsonRpcPayloadReader payloadReader;

    /**
     * Creates a parser with a default ObjectMapper and duplicate-member rejection disabled.
     */
    public DefaultJsonRpcResponseParser() {
        this(JsonMapper.builder().build(), false);
    }

    /**
     * Creates a parser with a default ObjectMapper and explicit duplicate-member policy.
     *
     * @param rejectDuplicateMembers {@code true} to reject duplicate members while parsing raw JSON input
     */
    public DefaultJsonRpcResponseParser(boolean rejectDuplicateMembers) {
        this(JsonMapper.builder().build(), rejectDuplicateMembers);
    }

    /**
     * Creates a parser with explicit mapper and duplicate-member policy.
     *
     * @param objectMapper            mapper used to parse raw JSON input
     * @param rejectDuplicateMembers  {@code true} to reject duplicate members while parsing raw JSON input
     */
    public DefaultJsonRpcResponseParser(ObjectMapper objectMapper, boolean rejectDuplicateMembers) {
        this.payloadReader = new JsonRpcPayloadReader(
            Objects.requireNonNull(objectMapper, "objectMapper"),
            rejectDuplicateMembers
        );
    }

    /**
     * Parses a raw JSON string response payload.
     *
     * @param payload raw JSON string
     * @return parsed incoming response envelope
     * @throws JsonRpcException when JSON parsing fails or payload shape is invalid
     */
    public JsonRpcIncomingResponseEnvelope parse(String payload) {
        if (payload == null) {
            throw invalidResponseEnvelope();
        }
        try {
            JsonNode node = payloadReader.readTree(payload);
            return parse(node);
        } catch (JacksonException ex) {
            throw invalidResponseEnvelope();
        }
    }

    /**
     * Parses raw JSON bytes response payload.
     *
     * @param payload raw JSON bytes
     * @return parsed incoming response envelope
     * @throws JsonRpcException when JSON parsing fails or payload shape is invalid
     */
    public JsonRpcIncomingResponseEnvelope parse(byte[] payload) {
        if (payload == null) {
            throw invalidResponseEnvelope();
        }
        try {
            JsonNode node = payloadReader.readTree(payload);
            return parse(node);
        } catch (JacksonException ex) {
            throw invalidResponseEnvelope();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonRpcIncomingResponseEnvelope parse(@Nullable JsonNode payload) {
        if (payload == null) {
            throw invalidResponseEnvelope();
        }
        if (payload.isObject()) {
            return JsonRpcIncomingResponseEnvelope.single(parseObject(payload));
        }
        if (!payload.isArray() || payload.isEmpty()) {
            throw invalidResponseEnvelope();
        }

        List<JsonRpcIncomingResponse> responses = new ArrayList<>(payload.size());
        for (JsonNode element : payload) {
            responses.add(parseObject(element));
        }
        return JsonRpcIncomingResponseEnvelope.batch(responses);
    }

    /**
     * Parses a response object and extracts known top-level members.
     *
     * @param node response object candidate
     * @return parsed incoming response
     */
    private JsonRpcIncomingResponse parseObject(JsonNode node) {
        if (!node.isObject()) {
            throw invalidResponseEnvelope();
        }

        JsonNode jsonrpcNode = node.get("jsonrpc");
        String jsonrpc = jsonrpcNode != null && jsonrpcNode.isString() ? jsonrpcNode.stringValue() : null;

        boolean idPresent = node.has("id");
        JsonNode id = node.get("id");

        boolean resultPresent = node.has("result");
        JsonNode result = node.get("result");

        boolean errorPresent = node.has("error");
        JsonNode error = node.get("error");

        return new JsonRpcIncomingResponse(
            node,
            jsonrpc,
            id,
            idPresent,
            result,
            resultPresent,
            error,
            errorPresent
        );
    }

    /**
     * Creates a standardized invalid-response exception.
     *
     * @return invalid-response exception
     */
    private JsonRpcException invalidResponseEnvelope() {
        return new JsonRpcException(JsonRpcErrorCode.INVALID_REQUEST, "Invalid response envelope");
    }

}
