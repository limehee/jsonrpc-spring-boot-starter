package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Default parser for incoming JSON-RPC response payloads.
 */
public class DefaultJsonRpcResponseParser implements JsonRpcResponseParser {

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
        if (node == null || !node.isObject()) {
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
