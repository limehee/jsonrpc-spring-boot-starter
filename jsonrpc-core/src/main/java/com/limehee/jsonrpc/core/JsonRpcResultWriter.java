package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;

public interface JsonRpcResultWriter {

    JsonNode write(@Nullable Object value);
}
