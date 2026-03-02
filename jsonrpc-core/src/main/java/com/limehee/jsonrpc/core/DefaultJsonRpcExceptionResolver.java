package com.limehee.jsonrpc.core;

/**
 * Default exception resolver that maps {@link JsonRpcException} and falls back to internal error.
 */
public class DefaultJsonRpcExceptionResolver implements JsonRpcExceptionResolver {

    private final boolean includeErrorData;

    /**
     * Creates resolver with hidden error data.
     */
    public DefaultJsonRpcExceptionResolver() {
        this(false);
    }

    /**
     * Creates resolver with configurable inclusion of custom error data.
     *
     * @param includeErrorData whether {@link JsonRpcException#getData()} should be exposed in responses
     */
    public DefaultJsonRpcExceptionResolver(boolean includeErrorData) {
        this.includeErrorData = includeErrorData;
    }

    /**
     * Resolves an exception into a JSON-RPC error.
     *
     * @param throwable thrown exception
     * @return mapped JSON-RPC error object
     */
    @Override
    public JsonRpcError resolve(Throwable throwable) {
        if (throwable instanceof JsonRpcException jsonRpcException) {
            return new JsonRpcError(
                    jsonRpcException.getCode(),
                    jsonRpcException.getMessage(),
                    includeErrorData ? jsonRpcException.getData() : null
            );
        }
        return JsonRpcError.of(JsonRpcErrorCode.INTERNAL_ERROR, JsonRpcConstants.MESSAGE_INTERNAL_ERROR);
    }
}
