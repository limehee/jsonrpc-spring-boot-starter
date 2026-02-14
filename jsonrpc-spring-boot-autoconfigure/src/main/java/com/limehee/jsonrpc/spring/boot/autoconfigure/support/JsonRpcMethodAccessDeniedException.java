package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcConstants;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcException;

public final class JsonRpcMethodAccessDeniedException extends JsonRpcException {

    public JsonRpcMethodAccessDeniedException() {
        super(JsonRpcErrorCode.METHOD_NOT_FOUND, JsonRpcConstants.MESSAGE_METHOD_NOT_FOUND);
    }
}
