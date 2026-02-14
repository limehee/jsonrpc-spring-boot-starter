package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {

    private String jsonrpc = JsonRpcConstants.VERSION;
    private JsonNode id;
    private JsonNode result;
    private JsonRpcError error;

    public JsonRpcResponse() {
    }

    public JsonRpcResponse(String jsonrpc, JsonNode id, JsonNode result, JsonRpcError error) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.result = result;
        this.error = error;
    }

    public static JsonRpcResponse success(JsonNode id, JsonNode result) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, result, null);
    }

    public static JsonRpcResponse error(JsonNode id, int code, String message) {
        return new JsonRpcResponse(JsonRpcConstants.VERSION, id, null, JsonRpcError.of(code, message));
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public JsonNode getId() {
        return id;
    }

    public void setId(JsonNode id) {
        this.id = id;
    }

    public JsonNode getResult() {
        return result;
    }

    public void setResult(JsonNode result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
