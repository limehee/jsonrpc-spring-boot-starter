package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;

public interface JsonRpcResultWriter {

    JsonNode write(Object value);
}
