package com.limehee.jsonrpc.spring.boot.autoconfigure.support;

import com.limehee.jsonrpc.core.JsonRpcInterceptor;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import org.springframework.core.Ordered;

import java.util.Set;

/**
 * Interceptor that enforces method-level access control using allowlist and denylist sets.
 * <p>
 * Evaluation order is:
 * </p>
 * <ol>
 * <li>If allowlist is non-empty, methods not included are rejected.</li>
 * <li>Methods explicitly present in denylist are always rejected.</li>
 * </ol>
 * <p>
 * Rejections are raised as {@link JsonRpcMethodAccessDeniedException}, which is mapped to
 * {@code METHOD_NOT_FOUND} for reduced endpoint discoverability.
 * </p>
 */
public final class JsonRpcMethodAccessInterceptor implements JsonRpcInterceptor, Ordered {

    private final Set<String> allowlist;
    private final Set<String> denylist;

    /**
     * Creates a new method access interceptor.
     *
     * @param allowlist methods that are allowed when the set is non-empty
     * @param denylist methods that are always denied
     */
    public JsonRpcMethodAccessInterceptor(Set<String> allowlist, Set<String> denylist) {
        this.allowlist = allowlist;
        this.denylist = denylist;
    }

    /**
     * Validates method access before invocation.
     *
     * @param request JSON-RPC request being processed
     * @throws JsonRpcMethodAccessDeniedException when access control rules reject the method
     */
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

    /**
     * Returns highest precedence so access control executes before most other interceptors.
     *
     * @return interceptor order value
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
