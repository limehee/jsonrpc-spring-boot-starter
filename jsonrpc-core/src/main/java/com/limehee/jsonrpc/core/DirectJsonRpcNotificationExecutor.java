package com.limehee.jsonrpc.core;

public class DirectJsonRpcNotificationExecutor implements JsonRpcNotificationExecutor {

    @Override
    public void execute(Runnable task) {
        task.run();
    }
}
