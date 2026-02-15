package com.limehee.jsonrpc.core;

import tools.jackson.databind.JsonNode;

public interface JsonRpcRequestParser {

    JsonRpcRequest parse(JsonNode node);
}
