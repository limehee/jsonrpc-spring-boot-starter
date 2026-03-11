package com.limehee.jsonrpc.core;

/**
 * Classifies integer JSON-RPC {@code error.code} values into semantic categories.
 * <p>
 * This utility describes what a code represents. It does not decide whether a response should be accepted or rejected
 * under a validation policy.
 * </p>
 */
public interface JsonRpcErrorClassifier {

    /**
     * Classifies the provided integer error code.
     *
     * @param code integer JSON-RPC error code
     * @return semantic error-code category
     */
    JsonRpcErrorCodeCategory classify(int code);

    /**
     * Checks whether the provided code is one of the JSON-RPC standard error codes.
     *
     * @param code integer JSON-RPC error code
     * @return {@code true} when the code belongs to the standard set
     */
    default boolean isStandard(int code) {
        return classify(code) == JsonRpcErrorCodeCategory.STANDARD;
    }

    /**
     * Checks whether the provided code belongs to the server-reserved range.
     *
     * @param code integer JSON-RPC error code
     * @return {@code true} when the code is within {@code -32099..-32000}
     */
    default boolean isServerErrorRange(int code) {
        return classify(code) == JsonRpcErrorCodeCategory.SERVER_RESERVED_RANGE;
    }

    /**
     * Checks whether the provided code is neither standard nor server-reserved.
     *
     * @param code integer JSON-RPC error code
     * @return {@code true} when the code falls into the custom category
     */
    default boolean isCustom(int code) {
        return classify(code) == JsonRpcErrorCodeCategory.CUSTOM;
    }
}
