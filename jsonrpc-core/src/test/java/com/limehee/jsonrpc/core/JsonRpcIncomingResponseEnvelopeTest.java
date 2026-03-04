package com.limehee.jsonrpc.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.IntNode;

class JsonRpcIncomingResponseEnvelopeTest {

    @Test
    void singleResponseReturnsEntryForSingleEnvelope() {
        JsonRpcIncomingResponse response = new JsonRpcIncomingResponse(
            IntNode.valueOf(1),
            "2.0",
            IntNode.valueOf(1),
            true,
            IntNode.valueOf(7),
            true,
            null,
            false
        );

        JsonRpcIncomingResponseEnvelope envelope = JsonRpcIncomingResponseEnvelope.single(response);

        assertFalse(envelope.isBatch());
        assertEquals(response, envelope.singleResponse().orElseThrow());
        assertEquals(1, envelope.responses().size());
    }

    @Test
    void singleResponseReturnsEmptyForBatchEnvelope() {
        JsonRpcIncomingResponse response = new JsonRpcIncomingResponse(
            IntNode.valueOf(1),
            "2.0",
            IntNode.valueOf(1),
            true,
            IntNode.valueOf(7),
            true,
            null,
            false
        );

        JsonRpcIncomingResponseEnvelope envelope = JsonRpcIncomingResponseEnvelope.batch(List.of(response));

        assertTrue(envelope.isBatch());
        assertTrue(envelope.singleResponse().isEmpty());
    }

    @Test
    void responsesListIsImmutableSnapshot() {
        JsonRpcIncomingResponse response = new JsonRpcIncomingResponse(
            IntNode.valueOf(1),
            "2.0",
            IntNode.valueOf(1),
            true,
            IntNode.valueOf(7),
            true,
            null,
            false
        );
        List<JsonRpcIncomingResponse> mutable = new ArrayList<>();
        mutable.add(response);

        JsonRpcIncomingResponseEnvelope envelope = JsonRpcIncomingResponseEnvelope.batch(mutable);
        mutable.clear();

        assertEquals(1, envelope.responses().size());
        assertThrows(UnsupportedOperationException.class, () -> envelope.responses().add(response));
    }
}
