package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcRequestParser {

    JsonRpcRequest parse(JsonNode node);
}
