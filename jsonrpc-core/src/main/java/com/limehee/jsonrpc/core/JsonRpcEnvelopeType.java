package com.limehee.jsonrpc.core;

/**
 * High-level JSON-RPC payload type classification.
 */
public enum JsonRpcEnvelopeType {
    /**
     * Payload should be handled by the request dispatch pipeline.
     */
    REQUEST,
    /**
     * Payload should be handled by response-side processing.
     */
    RESPONSE,
    /**
     * Payload shape cannot be treated as request or response.
     */
    INVALID
}
