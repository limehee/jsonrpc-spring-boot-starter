package com.limehee.jsonrpc.core;

import java.util.concurrent.Executor;

/**
 * Notification executor that delegates execution to a supplied {@link Executor}.
 */
public class ExecutorJsonRpcNotificationExecutor implements JsonRpcNotificationExecutor {

    private final Executor executor;

    /**
     * Creates an executor-backed notification runner.
     *
     * @param executor target executor for async execution
     */
    public ExecutorJsonRpcNotificationExecutor(Executor executor) {
        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
