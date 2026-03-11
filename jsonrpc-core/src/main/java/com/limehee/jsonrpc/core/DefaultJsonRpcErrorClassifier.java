package com.limehee.jsonrpc.core;

/**
 * Default classifier for integer JSON-RPC {@code error.code} values.
 */
public class DefaultJsonRpcErrorClassifier implements JsonRpcErrorClassifier {

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonRpcErrorCodeCategory classify(int code) {
        if (code == JsonRpcErrorCode.PARSE_ERROR
            || code == JsonRpcErrorCode.INVALID_REQUEST
            || code == JsonRpcErrorCode.METHOD_NOT_FOUND
            || code == JsonRpcErrorCode.INVALID_PARAMS
            || code == JsonRpcErrorCode.INTERNAL_ERROR) {
            return JsonRpcErrorCodeCategory.STANDARD;
        }
        if (code >= -32099 && code <= -32000) {
            return JsonRpcErrorCodeCategory.SERVER_RESERVED_RANGE;
        }
        return JsonRpcErrorCodeCategory.CUSTOM;
    }
}
