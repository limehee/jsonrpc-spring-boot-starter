package com.limehee.jsonrpc.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * Read-only index over parsed incoming responses for correlating batch responses with requests by {@code id}.
 * <p>
 * JSON-RPC 2.0 allows a server to return batch responses in any order, so a client has to match them by {@code id}.
 * This index groups responses by id and offers lookup and success/error views.
 * </p>
 * <h2>Id matching</h2>
 * <p>
 * String ids match by exact text and numeric ids match by numeric value, so {@code 1}, {@code 1.0} and a {@code long}
 * lookup of {@code 1} are the same id. A string id never matches a numeric id ({@code "1"} differs from {@code 1}).
 * Responses whose {@code id} is missing, {@code null}, or neither a string nor a number cannot be correlated; they
 * are excluded from lookup and exposed through {@link #unidentified()}.
 * </p>
 * <h2>Duplicate ids</h2>
 * <p>
 * More than one response with the same id is a server error, and picking one of them could silently return the
 * wrong result. Looking up a duplicated id therefore throws {@link IllegalStateException}. Use
 * {@link #hasDuplicateIds()} or {@link #duplicateIds()} to detect this before lookup.
 * </p>
 * <h2>Success and error views</h2>
 * <p>
 * A response counts as a success when it has {@code result} and no {@code error}, and as an error when it has
 * {@code error} and no {@code result}. Malformed responses with both or neither appear in neither view. Validate
 * responses with {@link JsonRpcResponseValidator} first when strict protocol checks are required.
 * </p>
 */
public final class JsonRpcResponseIndex {

    private final List<JsonRpcIncomingResponse> responses;
    private final Map<Object, List<JsonRpcIncomingResponse>> byId;
    private final List<JsonRpcIncomingResponse> unidentified;
    private final List<JsonRpcIncomingResponse> successes;
    private final List<JsonRpcIncomingResponse> errors;
    private final List<JsonNode> duplicateIds;

    private JsonRpcResponseIndex(List<JsonRpcIncomingResponse> responses) {
        this.responses = List.copyOf(responses);
        Map<Object, List<JsonRpcIncomingResponse>> grouped = new LinkedHashMap<>();
        List<JsonRpcIncomingResponse> unidentifiedResponses = new ArrayList<>();
        List<JsonRpcIncomingResponse> successResponses = new ArrayList<>();
        List<JsonRpcIncomingResponse> errorResponses = new ArrayList<>();
        for (JsonRpcIncomingResponse response : this.responses) {
            Object key = response.idPresent() ? keyOf(response.id()) : null;
            if (key == null) {
                unidentifiedResponses.add(response);
            } else {
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>(1)).add(response);
            }
            if (response.resultPresent() && !response.errorPresent()) {
                successResponses.add(response);
            } else if (response.errorPresent() && !response.resultPresent()) {
                errorResponses.add(response);
            }
        }
        List<JsonNode> duplicates = new ArrayList<>();
        Map<Object, List<JsonRpcIncomingResponse>> frozen = new LinkedHashMap<>();
        for (Map.Entry<Object, List<JsonRpcIncomingResponse>> entry : grouped.entrySet()) {
            List<JsonRpcIncomingResponse> group = entry.getValue();
            if (group.size() > 1) {
                duplicates.add(Objects.requireNonNull(group.get(0).id()));
            }
            frozen.put(entry.getKey(), List.copyOf(group));
        }
        this.byId = Collections.unmodifiableMap(frozen);
        this.unidentified = List.copyOf(unidentifiedResponses);
        this.successes = List.copyOf(successResponses);
        this.errors = List.copyOf(errorResponses);
        this.duplicateIds = List.copyOf(duplicates);
    }

    /**
     * Creates an index over the entries of a parsed envelope (single or batch).
     *
     * @param envelope parsed response envelope
     * @return response index
     */
    public static JsonRpcResponseIndex of(JsonRpcIncomingResponseEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        return new JsonRpcResponseIndex(envelope.responses());
    }

    /**
     * Creates an index over parsed responses.
     *
     * @param responses parsed response entries
     * @return response index
     */
    public static JsonRpcResponseIndex of(List<JsonRpcIncomingResponse> responses) {
        Objects.requireNonNull(responses, "responses");
        return new JsonRpcResponseIndex(responses);
    }

    /**
     * Finds the response for an id.
     *
     * @param id request id as a JSON string or number node
     * @return matching response, or empty when no response has this id
     * @throws IllegalArgumentException when {@code id} is not a string or number node
     * @throws IllegalStateException    when more than one response has this id
     */
    public Optional<JsonRpcIncomingResponse> findById(JsonNode id) {
        Objects.requireNonNull(id, "id");
        Object key = keyOf(id);
        if (key == null) {
            throw new IllegalArgumentException("id must be a JSON string or number: " + id);
        }
        List<JsonRpcIncomingResponse> group = byId.get(key);
        if (group == null) {
            return Optional.empty();
        }
        if (group.size() > 1) {
            throw new IllegalStateException("multiple responses share id " + id + " (" + group.size() + ")");
        }
        return Optional.of(group.get(0));
    }

    /**
     * Finds the response for a numeric id.
     *
     * @param id numeric request id
     * @return matching response, or empty when no response has this id
     * @throws IllegalStateException when more than one response has this id
     */
    public Optional<JsonRpcIncomingResponse> findById(long id) {
        return findById(JsonNodeFactory.instance.numberNode(id));
    }

    /**
     * Finds the response for a string id.
     *
     * @param id string request id
     * @return matching response, or empty when no response has this id
     * @throws IllegalStateException when more than one response has this id
     */
    public Optional<JsonRpcIncomingResponse> findById(String id) {
        Objects.requireNonNull(id, "id");
        return findById(JsonNodeFactory.instance.stringNode(id));
    }

    /**
     * Returns the response for an id, failing when it is missing.
     *
     * @param id request id as a JSON string or number node
     * @return matching response
     * @throws NoSuchElementException   when no response has this id
     * @throws IllegalArgumentException when {@code id} is not a string or number node
     * @throws IllegalStateException    when more than one response has this id
     */
    public JsonRpcIncomingResponse requireById(JsonNode id) {
        return findById(id).orElseThrow(() -> new NoSuchElementException("no response with id " + id));
    }

    /**
     * Returns the response for a numeric id, failing when it is missing.
     *
     * @param id numeric request id
     * @return matching response
     * @throws NoSuchElementException when no response has this id
     * @throws IllegalStateException  when more than one response has this id
     */
    public JsonRpcIncomingResponse requireById(long id) {
        return requireById(JsonNodeFactory.instance.numberNode(id));
    }

    /**
     * Returns the response for a string id, failing when it is missing.
     *
     * @param id string request id
     * @return matching response
     * @throws NoSuchElementException when no response has this id
     * @throws IllegalStateException  when more than one response has this id
     */
    public JsonRpcIncomingResponse requireById(String id) {
        Objects.requireNonNull(id, "id");
        return requireById(JsonNodeFactory.instance.stringNode(id));
    }

    /**
     * Returns whether at least one response has the given id.
     *
     * @param id request id as a JSON string or number node
     * @return {@code true} when the id is present, including duplicated ids
     * @throws IllegalArgumentException when {@code id} is not a string or number node
     */
    public boolean containsId(JsonNode id) {
        Objects.requireNonNull(id, "id");
        Object key = keyOf(id);
        if (key == null) {
            throw new IllegalArgumentException("id must be a JSON string or number: " + id);
        }
        return byId.containsKey(key);
    }

    /**
     * Returns the distinct correlatable ids in first-seen order.
     *
     * @return immutable id list
     */
    public List<JsonNode> ids() {
        List<JsonNode> ids = new ArrayList<>(byId.size());
        for (List<JsonRpcIncomingResponse> group : byId.values()) {
            ids.add(Objects.requireNonNull(group.get(0).id()));
        }
        return List.copyOf(ids);
    }

    /**
     * Returns all indexed responses in source order.
     *
     * @return immutable response list
     */
    public List<JsonRpcIncomingResponse> responses() {
        return responses;
    }

    /**
     * Returns the number of indexed responses, including unidentified ones.
     *
     * @return response count
     */
    public int size() {
        return responses.size();
    }

    /**
     * Returns responses with {@code result} and no {@code error}, in source order.
     *
     * @return immutable success list
     */
    public List<JsonRpcIncomingResponse> successes() {
        return successes;
    }

    /**
     * Returns responses with {@code error} and no {@code result}, in source order.
     *
     * @return immutable error list
     */
    public List<JsonRpcIncomingResponse> errors() {
        return errors;
    }

    /**
     * Returns responses that cannot be correlated: {@code id} missing, {@code null}, or neither a string nor a number.
     * <p>
     * A server returns {@code "id": null} when it could not determine the request id, for example on a parse error.
     * </p>
     *
     * @return immutable list in source order
     */
    public List<JsonRpcIncomingResponse> unidentified() {
        return unidentified;
    }

    /**
     * Returns whether any id is shared by more than one response.
     *
     * @return {@code true} when duplicates exist
     */
    public boolean hasDuplicateIds() {
        return !duplicateIds.isEmpty();
    }

    /**
     * Returns ids shared by more than one response, in first-seen order.
     *
     * @return immutable duplicate id list
     */
    public List<JsonNode> duplicateIds() {
        return duplicateIds;
    }

    private static @Nullable Object keyOf(@Nullable JsonNode id) {
        if (id == null) {
            return null;
        }
        if (id.isString()) {
            return id.asString();
        }
        if (id.isNumber()) {
            BigDecimal value = id.decimalValue().stripTrailingZeros();
            return value.signum() == 0 ? BigDecimal.ZERO : value;
        }
        return null;
    }
}
