package com.limehee.jsonrpc.core;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultJsonRpcEnvelopeClassifierTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final JsonRpcEnvelopeClassifier classifier = new DefaultJsonRpcEnvelopeClassifier();

    @Test
    void classifyReturnsRequestForMethodObject() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.REQUEST,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        {"jsonrpc":"2.0","method":"ping","id":1}
                        """))
        );
    }

    @Test
    void classifyReturnsRequestForParamsOnlyObject() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.REQUEST,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        {"jsonrpc":"2.0","params":{"x":1}}
                        """))
        );
    }

    @Test
    void classifyReturnsResponseForResultObject() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.RESPONSE,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        {"jsonrpc":"2.0","id":1,"result":true}
                        """))
        );
    }

    @Test
    void classifyReturnsResponseForErrorObject() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.RESPONSE,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        {"jsonrpc":"2.0","id":1,"error":{"code":-32600,"message":"Invalid Request"}}
                        """))
        );
    }

    @Test
    void classifyReturnsResponseWhenResponseHintsExistWithRequestFields() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.RESPONSE,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        {"jsonrpc":"2.0","method":"ping","result":true}
                        """))
        );
    }

    @Test
    void classifyReturnsInvalidForObjectWithoutHints() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.INVALID,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        {"jsonrpc":"2.0","id":1}
                        """))
        );
    }

    @Test
    void classifyReturnsRequestForHomogeneousRequestBatch() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.REQUEST,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        [
                          {"jsonrpc":"2.0","method":"a","id":1},
                          {"jsonrpc":"2.0","method":"b"}
                        ]
                        """))
        );
    }

    @Test
    void classifyReturnsResponseForHomogeneousResponseBatch() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.RESPONSE,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        [
                          {"jsonrpc":"2.0","id":1,"result":1},
                          {"jsonrpc":"2.0","id":2,"error":{"code":-32000,"message":"x"}}
                        ]
                        """))
        );
    }

    @Test
    void classifyReturnsInvalidForMixedBatch() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.INVALID,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        [
                          {"jsonrpc":"2.0","method":"a","id":1},
                          {"jsonrpc":"2.0","id":1,"result":1}
                        ]
                        """))
        );
    }

    @Test
    void classifyReturnsInvalidForNonObjectBatchEntry() throws Exception {
        assertEquals(
                JsonRpcEnvelopeType.INVALID,
                classifier.classify(OBJECT_MAPPER.readTree("""
                        [
                          {"jsonrpc":"2.0","id":1,"result":1},
                          3
                        ]
                        """))
        );
    }

    @Test
    void classifyReturnsInvalidForEmptyArrayOrPrimitiveOrNull() throws Exception {
        assertEquals(JsonRpcEnvelopeType.INVALID, classifier.classify(OBJECT_MAPPER.readTree("[]")));
        assertEquals(JsonRpcEnvelopeType.INVALID, classifier.classify(OBJECT_MAPPER.readTree("1")));
        assertEquals(JsonRpcEnvelopeType.INVALID, classifier.classify(null));
    }
}
