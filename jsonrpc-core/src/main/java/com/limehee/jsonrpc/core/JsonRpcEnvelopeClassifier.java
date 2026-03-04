package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Classifies raw JSON payloads into request/response/invalid envelope types.
 */
public interface JsonRpcEnvelopeClassifier {

    /**
     * Classifies the provided payload shape.
     *
     * @param payload raw JSON payload
     * @return envelope type classification
     */
    JsonRpcEnvelopeType classify(@Nullable JsonNode payload);
}
