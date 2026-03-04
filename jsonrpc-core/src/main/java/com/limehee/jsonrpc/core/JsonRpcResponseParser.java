package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Parses raw JSON payloads into incoming JSON-RPC response envelopes.
 */
public interface JsonRpcResponseParser {

    /**
     * Parses a single response object or a batch response array.
     *
     * @param payload raw JSON payload
     * @return parsed incoming response envelope
     * @throws JsonRpcException when payload is not a valid response envelope container
     */
    JsonRpcIncomingResponseEnvelope parse(@Nullable JsonNode payload);
}
