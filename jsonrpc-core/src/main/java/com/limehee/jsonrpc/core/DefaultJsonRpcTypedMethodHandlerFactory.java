package com.limehee.jsonrpc.core;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Default typed handler factory using binder/writer components for conversion.
 */
public class DefaultJsonRpcTypedMethodHandlerFactory implements JsonRpcTypedMethodHandlerFactory {

    private final JsonRpcParameterBinder parameterBinder;
    private final JsonRpcResultWriter resultWriter;

    /**
     * Creates a typed method handler factory.
     *
     * @param parameterBinder binder for converting params to Java values
     * @param resultWriter    serializer for converting Java return values to JSON
     */
    public DefaultJsonRpcTypedMethodHandlerFactory(JsonRpcParameterBinder parameterBinder,
        JsonRpcResultWriter resultWriter) {
        this.parameterBinder = Objects.requireNonNull(parameterBinder, "parameterBinder");
        this.resultWriter = Objects.requireNonNull(resultWriter, "resultWriter");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonRpcMethodHandler noParams(Supplier<?> method) {
        return params -> {
            validateNoParams(params);
            Object result = method.get();
            return resultWriter.write(result);
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <P> JsonRpcMethodHandler unary(Class<P> paramType, Function<P, ?> method) {
        return params -> {
            P boundParams = parameterBinder.bind(params, paramType);
            Object result = method.apply(boundParams);
            return resultWriter.write(result);
        };
    }

    /**
     * Ensures params are absent for zero-argument handlers.
     *
     * @param params params payload to validate
     * @throws JsonRpcException when unexpected params are provided
     */
    private void validateNoParams(@Nullable JsonNode params) {
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
