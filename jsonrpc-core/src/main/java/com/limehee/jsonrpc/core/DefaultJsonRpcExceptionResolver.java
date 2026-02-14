package com.limehee.jsonrpc.core;

public class DefaultJsonRpcExceptionResolver implements JsonRpcExceptionResolver {

    private final boolean includeErrorData;

    public DefaultJsonRpcExceptionResolver() {
        this(true);
    }

    public DefaultJsonRpcExceptionResolver(boolean includeErrorData) {
        this.includeErrorData = includeErrorData;
    }

    @Override
    public JsonRpcError resolve(Throwable throwable) {
        if (throwable instanceof JsonRpcException jsonRpcException) {
            return new JsonRpcError(
                    jsonRpcException.getCode(),
                    jsonRpcException.getMessage(),
                    includeErrorData ? jsonRpcException.getData() : null
            );
        }
        if (throwable instanceof IllegalArgumentException ex) {
            String message = ex.getMessage() == null ? JsonRpcConstants.MESSAGE_INVALID_PARAMS : ex.getMessage();
            return JsonRpcError.of(JsonRpcErrorCode.INVALID_PARAMS, message);
        }
        return JsonRpcError.of(JsonRpcErrorCode.INTERNAL_ERROR, JsonRpcConstants.MESSAGE_INTERNAL_ERROR);
    }
}
