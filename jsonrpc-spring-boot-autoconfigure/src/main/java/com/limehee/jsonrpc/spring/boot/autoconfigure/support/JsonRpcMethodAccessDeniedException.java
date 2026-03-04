package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcConstants;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcException;

/**
 * Exception raised when a JSON-RPC method is rejected by allowlist/denylist rules.
 * <p>
 * The exception intentionally maps to {@link JsonRpcErrorCode#METHOD_NOT_FOUND} so callers do not learn whether the
 * method exists but is blocked by policy.
 * </p>
 */
public final class JsonRpcMethodAccessDeniedException extends JsonRpcException {

    /**
     * Creates an access denied exception mapped to the standard "Method not found" JSON-RPC error.
     */
    public JsonRpcMethodAccessDeniedException() {
        super(JsonRpcErrorCode.METHOD_NOT_FOUND, JsonRpcConstants.MESSAGE_METHOD_NOT_FOUND);
    }
}
