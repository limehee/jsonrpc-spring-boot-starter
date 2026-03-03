package com.limehee.jsonrpc.core;

import java.util.List;
import java.util.Optional;

/**
 * Parsed incoming response envelope for single or batch payloads.
 */
public final class JsonRpcIncomingResponseEnvelope {

    private final boolean batch;
    private final List<JsonRpcIncomingResponse> responses;

    private JsonRpcIncomingResponseEnvelope(boolean batch, List<JsonRpcIncomingResponse> responses) {
        this.batch = batch;
        this.responses = List.copyOf(responses);
    }

    /**
     * Creates a single-response envelope.
     *
     * @param response parsed response entry
     * @return single envelope
     */
    public static JsonRpcIncomingResponseEnvelope single(JsonRpcIncomingResponse response) {
        return new JsonRpcIncomingResponseEnvelope(false, List.of(response));
    }

    /**
     * Creates a batch-response envelope.
     *
     * @param responses parsed response entries
     * @return batch envelope
     */
    public static JsonRpcIncomingResponseEnvelope batch(List<JsonRpcIncomingResponse> responses) {
        return new JsonRpcIncomingResponseEnvelope(true, responses);
    }

    /**
     * Indicates whether the source payload was a batch response.
     *
     * @return {@code true} when batch
     */
    public boolean isBatch() {
        return batch;
    }

    /**
     * Returns immutable parsed entries.
     *
     * @return parsed response list
     */
    public List<JsonRpcIncomingResponse> responses() {
        return responses;
    }

    /**
     * Returns the single entry when available.
     *
     * @return optional single response
     */
    public Optional<JsonRpcIncomingResponse> singleResponse() {
        if (batch || responses.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(responses.get(0));
    }
}
