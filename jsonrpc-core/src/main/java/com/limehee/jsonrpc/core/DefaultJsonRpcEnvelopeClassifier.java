package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Default envelope classifier using top-level field presence heuristics.
 */
public class DefaultJsonRpcEnvelopeClassifier implements JsonRpcEnvelopeClassifier {

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonRpcEnvelopeType classify(@Nullable JsonNode payload) {
        if (payload == null) {
            return JsonRpcEnvelopeType.INVALID;
        }
        if (payload.isObject()) {
            return classifyObject(payload);
        }
        if (!payload.isArray() || payload.isEmpty()) {
            return JsonRpcEnvelopeType.INVALID;
        }

        JsonRpcEnvelopeType batchType = null;
        for (JsonNode element : payload) {
            JsonRpcEnvelopeType current = classifyObject(element);
            if (current == JsonRpcEnvelopeType.INVALID) {
                return JsonRpcEnvelopeType.INVALID;
            }
            if (batchType == null) {
                batchType = current;
                continue;
            }
            if (batchType != current) {
                return JsonRpcEnvelopeType.INVALID;
            }
        }
        return batchType == null ? JsonRpcEnvelopeType.INVALID : batchType;
    }

    /**
     * Classifies a single object node by request/response marker fields.
     * <p>
     * When request and response hints coexist, response classification takes precedence to keep routing aligned with
     * response-side validation policies.
     *
     * @param node object node candidate
     * @return envelope classification
     */
    private JsonRpcEnvelopeType classifyObject(JsonNode node) {
        if (!node.isObject()) {
            return JsonRpcEnvelopeType.INVALID;
        }

        boolean hasResponseHint = node.has("result") || node.has("error");
        boolean hasRequestHint = node.has("method") || node.has("params");
        if (hasResponseHint) {
            return JsonRpcEnvelopeType.RESPONSE;
        }
        if (hasRequestHint) {
            return JsonRpcEnvelopeType.REQUEST;
        }
        return JsonRpcEnvelopeType.INVALID;
    }
}
