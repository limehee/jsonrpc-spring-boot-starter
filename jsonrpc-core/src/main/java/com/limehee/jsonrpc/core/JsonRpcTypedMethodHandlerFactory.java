package com.limehee.jsonrpc.core;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Creates {@link JsonRpcMethodHandler} instances from strongly-typed Java callbacks.
 * <p>
 * This factory bridges the JSON-centric dispatcher contract and user-friendly Java method signatures.
 * Implementations are responsible for parameter binding and result serialization through the configured
 * binder/writer strategy.
 */
public interface JsonRpcTypedMethodHandlerFactory {

    /**
     * Creates a handler for methods that must not receive any JSON-RPC {@code params} payload.
     *
     * @param method callback to invoke when the method is called
     * @return method handler that validates the absence of parameters and serializes the callback result
     */
    JsonRpcMethodHandler noParams(Supplier<?> method);

    /**
     * Creates a handler for methods that accept a single typed argument.
     *
     * @param paramType target Java type used for binding the incoming {@code params}
     * @param method callback to invoke after binding succeeds
     * @param <P> bound argument type
     * @return method handler that binds one argument and serializes the callback result
     */
    <P> JsonRpcMethodHandler unary(Class<P> paramType, Function<P, ?> method);
}
