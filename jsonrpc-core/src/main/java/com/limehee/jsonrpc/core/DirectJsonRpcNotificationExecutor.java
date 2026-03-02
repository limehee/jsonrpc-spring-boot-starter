package com.limehee.jsonrpc.core;

/**
 * Notification executor that runs tasks synchronously on the caller thread.
 */
public class DirectJsonRpcNotificationExecutor implements JsonRpcNotificationExecutor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable task) {
        task.run();
    }
}
