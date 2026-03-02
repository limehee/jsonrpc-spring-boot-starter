package com.limehee.jsonrpc.core;

/**
 * Conflict strategy for duplicate method registration.
 */
public enum JsonRpcMethodRegistrationConflictPolicy {
    /**
     * Reject duplicate registration attempts.
     */
    REJECT,
    /**
     * Replace existing handlers when the same method is registered again.
     */
    REPLACE
}
