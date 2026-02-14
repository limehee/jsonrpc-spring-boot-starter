package com.limehee.jsonrpc.spring.webmvc;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultJsonRpcHttpStatusStrategyTest {

    private final DefaultJsonRpcHttpStatusStrategy strategy = new DefaultJsonRpcHttpStatusStrategy();

    @Test
    void statusForSingleAlwaysReturnsOk() {
        JsonRpcResponse success = JsonRpcResponse.success(IntNode.valueOf(1), TextNode.valueOf("pong"));
        JsonRpcResponse error = JsonRpcResponse.error(IntNode.valueOf(1), JsonRpcErrorCode.INVALID_REQUEST, "invalid");

        assertEquals(HttpStatus.OK, strategy.statusForSingle(success));
        assertEquals(HttpStatus.OK, strategy.statusForSingle(error));
    }

    @Test
    void statusForBatchReturnsOk() {
        List<JsonRpcResponse> responses = List.of(
                JsonRpcResponse.success(IntNode.valueOf(1), TextNode.valueOf("ok")),
                JsonRpcResponse.error(IntNode.valueOf(2), JsonRpcErrorCode.METHOD_NOT_FOUND, "not found")
        );

        assertEquals(HttpStatus.OK, strategy.statusForBatch(responses));
    }

    @Test
    void statusForSpecialCasesUsesDefaultValues() {
        assertEquals(HttpStatus.NO_CONTENT, strategy.statusForNotificationOnly());
        assertEquals(HttpStatus.OK, strategy.statusForParseError());
        assertEquals(HttpStatus.OK, strategy.statusForRequestTooLarge());
    }
}
