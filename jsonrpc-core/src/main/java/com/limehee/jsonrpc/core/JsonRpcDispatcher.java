package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonRpcDispatcher {

    private final Map<String, JsonRpcMethodHandler> handlers = new ConcurrentHashMap<>();

    public void register(String method, JsonRpcMethodHandler handler) {
        handlers.put(method, handler);
    }

    public JsonRpcResponse dispatch(JsonRpcRequest request) {
        if (request == null
                || request.getMethod() == null
                || request.getMethod().isBlank()
                || !JsonRpcConstants.VERSION.equals(request.getJsonrpc())) {
            return JsonRpcResponse.error(null, JsonRpcErrorCode.INVALID_REQUEST, "Invalid Request");
        }

        JsonRpcMethodHandler handler = handlers.get(request.getMethod());
        if (handler == null) {
            return JsonRpcResponse.error(request.getId(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Method not found");
        }

        try {
            JsonNode result = handler.handle(request.getParams());
            return JsonRpcResponse.success(request.getId(), result);
        } catch (IllegalArgumentException ex) {
            return JsonRpcResponse.error(request.getId(), JsonRpcErrorCode.INVALID_PARAMS, ex.getMessage());
        } catch (Exception ex) {
            return JsonRpcResponse.error(request.getId(), JsonRpcErrorCode.INTERNAL_ERROR, "Internal error");
        }
    }
}
