package com.limehee.jsonrpc.core;

public interface JsonRpcNotificationExecutor {

    void execute(Runnable task);
}
