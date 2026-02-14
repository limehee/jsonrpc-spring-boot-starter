package com.limehee.jsonrpc.core;

import java.util.concurrent.Executor;

public class ExecutorJsonRpcNotificationExecutor implements JsonRpcNotificationExecutor {

    private final Executor executor;

    public ExecutorJsonRpcNotificationExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
