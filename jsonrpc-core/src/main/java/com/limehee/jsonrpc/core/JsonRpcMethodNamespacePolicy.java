package com.limehee.jsonrpc.core;

public enum JsonRpcMethodNamespacePolicy {
    /**
     * Kept for backward compatibility.
     * Reserved methods starting with {@code rpc.} are always rejected by the registry.
     */
    @Deprecated
    ALLOW_ALL,
    /**
     * Reject methods with reserved {@code rpc.} prefix.
     */
    DISALLOW_RPC_PREFIX
}
