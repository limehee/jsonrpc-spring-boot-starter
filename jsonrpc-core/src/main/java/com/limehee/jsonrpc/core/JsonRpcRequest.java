package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcRequest {

    private String jsonrpc = JsonRpcConstants.VERSION;
    private JsonNode id;
    private String method;
    private JsonNode params;

    public JsonRpcRequest() {
    }

    public JsonRpcRequest(String jsonrpc, JsonNode id, String method, JsonNode params) {
        this.jsonrpc = jsonrpc;
        this.id = id;
        this.method = method;
        this.params = params;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }
}
