package com.limehee.jsonrpc.spring.webmvc;

import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public interface JsonRpcHttpStatusStrategy {

    HttpStatus statusForSingle(JsonRpcResponse response);

    HttpStatus statusForBatch(List<JsonRpcResponse> responses);

    HttpStatus statusForNotificationOnly();

    HttpStatus statusForParseError();

    HttpStatus statusForRequestTooLarge();
}
