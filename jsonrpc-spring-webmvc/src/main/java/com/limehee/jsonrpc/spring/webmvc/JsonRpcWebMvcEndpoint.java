package com.limehee.jsonrpc.spring.webmvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class JsonRpcWebMvcEndpoint {

    private final JsonRpcDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final JsonRpcHttpStatusStrategy httpStatusStrategy;
    private final int maxRequestBytes;

    public JsonRpcWebMvcEndpoint(
            JsonRpcDispatcher dispatcher,
            ObjectMapper objectMapper,
            JsonRpcHttpStatusStrategy httpStatusStrategy,
            int maxRequestBytes
    ) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
        this.httpStatusStrategy = httpStatusStrategy;
        this.maxRequestBytes = maxRequestBytes;
    }

    @PostMapping("${jsonrpc.path:/jsonrpc}")
    public ResponseEntity<JsonNode> invoke(@RequestBody(required = false) byte[] body) {
        if (body == null || body.length == 0) {
            return singleErrorResponse(dispatcher.parseErrorResponse(), HttpStatus.OK);
        }
        if (body.length > maxRequestBytes) {
            JsonRpcResponse response = JsonRpcResponse.error(
                    null,
                    JsonRpcErrorCode.INVALID_REQUEST,
                    "Request payload too large");
            return singleErrorResponse(response, HttpStatus.OK);
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            return singleErrorResponse(dispatcher.parseErrorResponse(), HttpStatus.OK);
        } catch (IOException ex) {
            return singleErrorResponse(dispatcher.parseErrorResponse(), HttpStatus.OK);
        }

        JsonRpcDispatchResult result = dispatcher.dispatch(payload);
        if (!result.hasResponse()) {
            return ResponseEntity.status(httpStatusStrategy.statusForNotificationOnly()).build();
        }

        if (result.isBatch()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            result.responses().forEach(response -> arrayNode.add(objectMapper.valueToTree(response)));
            return ResponseEntity.status(httpStatusStrategy.statusForBatch(result.responses())).body(arrayNode);
        }

        JsonRpcResponse single = result.singleResponse().orElseThrow();
        return ResponseEntity.status(httpStatusStrategy.statusForSingle(single)).body(objectMapper.valueToTree(single));
    }

    private ResponseEntity<JsonNode> singleErrorResponse(JsonRpcResponse response, HttpStatus status) {
        return ResponseEntity.status(status).body(objectMapper.valueToTree(response));
    }
}
