package com.limehee.jsonrpc.core;

/**
 * Validates parsed incoming JSON-RPC responses against protocol rules.
 */
public interface JsonRpcResponseValidator {

    /**
     * Validates a single parsed incoming response.
     *
     * @param response parsed response
     * @throws JsonRpcException when the response violates configured validation constraints
     */
    void validate(JsonRpcIncomingResponse response);
}
