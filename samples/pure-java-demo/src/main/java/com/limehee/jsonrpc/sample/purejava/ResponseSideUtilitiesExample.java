package com.limehee.jsonrpc.sample.purejava;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.DefaultJsonRpcEnvelopeClassifier;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseValidator;
import com.limehee.jsonrpc.core.JsonRpcEnvelopeClassifier;
import com.limehee.jsonrpc.core.JsonRpcEnvelopeType;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponse;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponseEnvelope;
import com.limehee.jsonrpc.core.JsonRpcResponseParser;
import com.limehee.jsonrpc.core.JsonRpcResponseValidationOptions;
import com.limehee.jsonrpc.core.JsonRpcResponseValidator;

import java.util.ArrayList;
import java.util.List;

public final class ResponseSideUtilitiesExample {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final JsonRpcEnvelopeClassifier classifier;
    private final JsonRpcResponseParser parser;
    private final JsonRpcResponseValidator validator;

    public ResponseSideUtilitiesExample(JsonRpcResponseValidationOptions options) {
        this.classifier = new DefaultJsonRpcEnvelopeClassifier();
        this.parser = new DefaultJsonRpcResponseParser();
        this.validator = new DefaultJsonRpcResponseValidator(options);
    }

    public Result inspect(String rawMessage) throws JacksonException {
        JsonNode payload = OBJECT_MAPPER.readTree(rawMessage);
        JsonRpcEnvelopeType envelopeType = classifier.classify(payload);
        if (envelopeType != JsonRpcEnvelopeType.RESPONSE) {
            return new Result(envelopeType, List.of());
        }

        JsonRpcIncomingResponseEnvelope envelope = parser.parse(payload);
        List<JsonRpcIncomingResponse> validated = new ArrayList<>(envelope.responses().size());
        for (JsonRpcIncomingResponse response : envelope.responses()) {
            validator.validate(response);
            validated.add(response);
        }
        return new Result(envelopeType, validated);
    }

    public record Result(JsonRpcEnvelopeType envelopeType, List<JsonRpcIncomingResponse> responses) {

    }
}
