package com.limehee.jsonrpc.core;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Parsed incoming JSON-RPC response model preserving field presence semantics.
 *
 * @param source        original response object node; may be {@code null}
 * @param jsonrpc       protocol version field value when textual; otherwise {@code null}
 * @param id            id field value when present; may be {@code null}
 * @param idPresent     whether the response explicitly contained an {@code id} member
 * @param result        result field value when present; may be {@code null}
 * @param resultPresent whether the response explicitly contained a {@code result} member
 * @param error         error field value when present; may be {@code null}
 * @param errorPresent  whether the response explicitly contained an {@code error} member
 */
public record JsonRpcIncomingResponse(
    @Nullable JsonNode source,
    @Nullable String jsonrpc,
    @Nullable JsonNode id,
    boolean idPresent,
    @Nullable JsonNode result,
    boolean resultPresent,
    @Nullable JsonNode error,
    boolean errorPresent
) {

    /**
     * Creates a response model without the original source node.
     *
     * @param jsonrpc       protocol version field value when textual; otherwise {@code null}
     * @param id            id field value when present; may be {@code null}
     * @param idPresent     whether the response explicitly contained an {@code id} member
     * @param result        result field value when present; may be {@code null}
     * @param resultPresent whether the response explicitly contained a {@code result} member
     * @param error         error field value when present; may be {@code null}
     * @param errorPresent  whether the response explicitly contained an {@code error} member
     */
    public JsonRpcIncomingResponse(
        @Nullable String jsonrpc,
        @Nullable JsonNode id,
        boolean idPresent,
        @Nullable JsonNode result,
        boolean resultPresent,
        @Nullable JsonNode error,
        boolean errorPresent
    ) {
        this(null, jsonrpc, id, idPresent, result, resultPresent, error, errorPresent);
    }

}
