package com.limehee.jsonrpc.core;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Notification executor that delegates execution to a supplied {@link Executor}.
 */
public class ExecutorJsonRpcNotificationExecutor implements JsonRpcNotificationExecutor {

    private final Executor executor;

    /**
     * Creates an executor-backed notification runner.
     *
     * @param executor target executor that performs delegated notification execution
     */
    public ExecutorJsonRpcNotificationExecutor(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }
}
