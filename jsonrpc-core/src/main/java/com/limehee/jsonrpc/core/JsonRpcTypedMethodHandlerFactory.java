package com.limehee.jsonrpc.core;

import java.util.function.Function;
import java.util.function.Supplier;

public interface JsonRpcTypedMethodHandlerFactory {

    JsonRpcMethodHandler noParams(Supplier<?> method);

    <P> JsonRpcMethodHandler unary(Class<P> paramType, Function<P, ?> method);
}
