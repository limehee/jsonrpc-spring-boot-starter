package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultJsonRpcTypedMethodHandlerFactory implements JsonRpcTypedMethodHandlerFactory {

    private final JsonRpcParameterBinder parameterBinder;
    private final JsonRpcResultWriter resultWriter;

    public DefaultJsonRpcTypedMethodHandlerFactory(JsonRpcParameterBinder parameterBinder, JsonRpcResultWriter resultWriter) {
        this.parameterBinder = parameterBinder;
        this.resultWriter = resultWriter;
    }

    @Override
    public JsonRpcMethodHandler noParams(Supplier<?> method) {
        return params -> {
            validateNoParams(params);
            Object result = method.get();
            return resultWriter.write(result);
        };
    }

    @Override
    public <P> JsonRpcMethodHandler unary(Class<P> paramType, Function<P, ?> method) {
        return params -> {
            P boundParams = parameterBinder.bind(params, paramType);
            Object result = method.apply(boundParams);
            return resultWriter.write(result);
        };
    }

    private void validateNoParams(JsonNode params) {
        if (params == null || params.isNull()) {
            return;
        }
        if (params.isArray() && params.isEmpty()) {
            return;
        }
        if (params.isObject() && params.isEmpty()) {
            return;
        }
        throw new JsonRpcException(JsonRpcErrorCode.INVALID_PARAMS, JsonRpcConstants.MESSAGE_INVALID_PARAMS);
    }
}
