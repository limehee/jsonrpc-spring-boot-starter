package com.limehee.jsonrpc.spring.webmvc;

import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.springframework.http.HttpStatus;

import java.util.List;

public class DefaultJsonRpcHttpStatusStrategy implements JsonRpcHttpStatusStrategy {

    @Override
    public HttpStatus statusForSingle(JsonRpcResponse response) {
        return HttpStatus.OK;
    }

    @Override
    public HttpStatus statusForBatch(List<JsonRpcResponse> responses) {
        return HttpStatus.OK;
    }

    @Override
    public HttpStatus statusForNotificationOnly() {
        return HttpStatus.NO_CONTENT;
    }
}
