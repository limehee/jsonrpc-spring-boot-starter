package com.limehee.jsonrpc.sample.purejava;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.DefaultJsonRpcEnvelopeClassifier;
import com.limehee.jsonrpc.core.DefaultJsonRpcErrorClassifier;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseValidator;
import com.limehee.jsonrpc.core.JsonRpcEnvelopeClassifier;
import com.limehee.jsonrpc.core.JsonRpcErrorClassifier;
import com.limehee.jsonrpc.core.JsonRpcErrorCodeCategory;
import com.limehee.jsonrpc.core.JsonRpcEnvelopeType;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponse;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponseEnvelope;
import com.limehee.jsonrpc.core.JsonRpcResponseValidationOptions;
import com.limehee.jsonrpc.core.JsonRpcResponseValidator;

import java.util.ArrayList;
import java.util.List;

public final class ResponseSideUtilitiesExample {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final JsonRpcEnvelopeClassifier classifier;
    private final JsonRpcErrorClassifier errorClassifier;
    private final DefaultJsonRpcResponseParser parser;
    private final JsonRpcResponseValidator validator;

    public ResponseSideUtilitiesExample(JsonRpcResponseValidationOptions options) {
        this.classifier = new DefaultJsonRpcEnvelopeClassifier();
        this.errorClassifier = new DefaultJsonRpcErrorClassifier();
        this.parser = new DefaultJsonRpcResponseParser(options.rejectDuplicateMembers());
        this.validator = new DefaultJsonRpcResponseValidator(options);
    }

    public Result inspect(String rawMessage) throws JacksonException {
        JsonNode payload = OBJECT_MAPPER.readTree(rawMessage);
        JsonRpcEnvelopeType envelopeType = classifier.classify(payload);
        if (envelopeType != JsonRpcEnvelopeType.RESPONSE) {
            return new Result(envelopeType, List.of());
        }

        JsonRpcIncomingResponseEnvelope envelope = parser.parse(rawMessage);
        List<JsonRpcIncomingResponse> validated = new ArrayList<>(envelope.responses().size());
        for (JsonRpcIncomingResponse response : envelope.responses()) {
            validator.validate(response);
            validated.add(response);
        }
        return new Result(envelopeType, validated);
    }

    /**
     * Classifies a validated or application-provided integer JSON-RPC error code.
     *
     * @param code integer JSON-RPC error code
     * @return semantic error-code category
     */
    public JsonRpcErrorCodeCategory classifyErrorCode(int code) {
        return errorClassifier.classify(code);
    }

    /**
     * Parses, validates, and classifies integer response error codes found in the provided payload.
     *
     * @param rawMessage raw JSON message that may contain response envelopes
     * @return classified error-code entries extracted from validated responses
     * @throws JacksonException when parsing or validation fails
     */
    public List<ClassifiedErrorCode> classifyValidatedErrorCodes(String rawMessage) throws JacksonException {
        Result result = inspect(rawMessage);
        if (result.envelopeType() != JsonRpcEnvelopeType.RESPONSE) {
            return List.of();
        }

        List<ClassifiedErrorCode> classified = new ArrayList<>();
        for (JsonRpcIncomingResponse response : result.responses()) {
            if (!response.errorPresent() || response.error() == null || !response.error().isObject()) {
                continue;
            }
            JsonNode code = response.error().get("code");
            if (code == null || !code.isNumber() || code.isFloatingPointNumber()) {
                continue;
            }
            classified.add(new ClassifiedErrorCode(code.intValue(), errorClassifier.classify(code.intValue())));
        }
        return classified;
    }

    public record Result(JsonRpcEnvelopeType envelopeType, List<JsonRpcIncomingResponse> responses) {

    }

    /**
     * Classified integer error-code entry extracted from a validated response.
     *
     * @param code integer JSON-RPC error code
     * @param category semantic classifier result
     */
    public record ClassifiedErrorCode(int code, JsonRpcErrorCodeCategory category) {

    }
}
