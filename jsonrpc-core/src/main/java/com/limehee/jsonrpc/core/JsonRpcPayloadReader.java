package com.limehee.jsonrpc.core;

import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads raw JSON payloads with optional duplicate-member rejection.
 */
public final class JsonRpcPayloadReader {

    private final ObjectMapper objectMapper;
    private final ObjectMapper strictObjectMapper;
    private final boolean rejectDuplicateMembers;

    /**
     * Creates a reader bound to a mapper and duplicate-member policy.
     *
     * @param objectMapper           mapper used for JSON parsing
     * @param rejectDuplicateMembers {@code true} to reject duplicate object members
     */
    public JsonRpcPayloadReader(ObjectMapper objectMapper, boolean rejectDuplicateMembers) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.strictObjectMapper = objectMapper.rebuild()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
        this.rejectDuplicateMembers = rejectDuplicateMembers;
    }

    /**
     * Reads JSON from text.
     *
     * @param payload raw JSON text
     * @return parsed JSON node
     * @throws JacksonException when payload cannot be parsed as JSON
     */
    public JsonNode readTree(String payload) throws JacksonException {
        return parserMapper().readTree(payload);
    }

    /**
     * Reads JSON from bytes.
     *
     * @param payload raw JSON bytes
     * @return parsed JSON node
     * @throws JacksonException when payload cannot be parsed as JSON
     */
    public JsonNode readTree(byte[] payload) throws JacksonException {
        return parserMapper().readTree(payload);
    }

    private ObjectMapper parserMapper() {
        return rejectDuplicateMembers ? strictObjectMapper : objectMapper;
    }
}
