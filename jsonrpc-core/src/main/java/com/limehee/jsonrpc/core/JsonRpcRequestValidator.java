package com.limehee.jsonrpc.core;

/**
 * Validates parsed JSON-RPC requests against protocol rules.
 */
public interface JsonRpcRequestValidator {

    /**
     * Validates one request.
     *
     * @param request request model to validate
     * @throws JsonRpcException when the request violates JSON-RPC rules
     */
    void validate(JsonRpcRequest request);
}
