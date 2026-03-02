package com.limehee.jsonrpc.core;

/**
 * Executes notification tasks that do not return a JSON-RPC response.
 */
public interface JsonRpcNotificationExecutor {

    /**
     * Executes a notification task.
     *
     * @param task notification execution logic
     */
    void execute(Runnable task);
}
