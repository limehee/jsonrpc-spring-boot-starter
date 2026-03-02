package com.limehee.jsonrpc.spring.webmvc;

import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Default HTTP status strategy for JSON-RPC over HTTP.
 * <p>
 * This implementation intentionally returns {@code 200 OK} for all JSON-RPC response payloads,
 * including error payloads, and returns {@code 204 NO_CONTENT} for notification-only requests.
 * This behavior aligns with common JSON-RPC-over-HTTP conventions where protocol-level errors are
 * represented inside the JSON-RPC response body rather than by transport status codes.
 * </p>
 */
public class DefaultJsonRpcHttpStatusStrategy implements JsonRpcHttpStatusStrategy {

    /**
     * Returns {@link HttpStatus#OK} for single responses.
     *
     * @param response single JSON-RPC response payload
     * @return {@link HttpStatus#OK}
     */
    @Override
    public HttpStatus statusForSingle(JsonRpcResponse response) {
        return HttpStatus.OK;
    }

    /**
     * Returns {@link HttpStatus#OK} for batch responses.
     *
     * @param responses JSON-RPC response payloads for a batch request
     * @return {@link HttpStatus#OK}
     */
    @Override
    public HttpStatus statusForBatch(List<JsonRpcResponse> responses) {
        return HttpStatus.OK;
    }

    /**
     * Returns {@link HttpStatus#NO_CONTENT} when no JSON-RPC response payload is generated.
     *
     * @return {@link HttpStatus#NO_CONTENT}
     */
    @Override
    public HttpStatus statusForNotificationOnly() {
        return HttpStatus.NO_CONTENT;
    }

    /**
     * Returns {@link HttpStatus#OK} for parse errors represented as JSON-RPC error responses.
     *
     * @return {@link HttpStatus#OK}
     */
    @Override
    public HttpStatus statusForParseError() {
        return HttpStatus.OK;
    }

    /**
     * Returns {@link HttpStatus#OK} for oversized requests represented as JSON-RPC error responses.
     *
     * @return {@link HttpStatus#OK}
     */
    @Override
    public HttpStatus statusForRequestTooLarge() {
        return HttpStatus.OK;
    }
}
