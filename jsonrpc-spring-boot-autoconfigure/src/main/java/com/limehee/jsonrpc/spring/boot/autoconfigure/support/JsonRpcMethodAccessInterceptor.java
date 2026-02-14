package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import org.springframework.core.Ordered;

import java.util.Set;

public final class JsonRpcMethodAccessInterceptor implements JsonRpcInterceptor, Ordered {

    private final Set<String> allowlist;
    private final Set<String> denylist;

    public JsonRpcMethodAccessInterceptor(Set<String> allowlist, Set<String> denylist) {
        this.allowlist = allowlist;
        this.denylist = denylist;
    }

    @Override
    public void beforeInvoke(JsonRpcRequest request) {
        String method = request.method();

        if (!allowlist.isEmpty() && !allowlist.contains(method)) {
            throw new JsonRpcMethodAccessDeniedException();
        }
        if (denylist.contains(method)) {
            throw new JsonRpcMethodAccessDeniedException();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
