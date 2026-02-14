package com.limehee.jsonrpc.core;

public interface JsonRpcExceptionResolver {

    JsonRpcError resolve(Throwable throwable);
}
