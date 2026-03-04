package com.limehee.jsonrpc.sample.purejava;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcParamsTypeViolationCodePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PureJavaDemoApplicationTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @Test
    void returnsExpectedResultForSingleRequest() throws JacksonException {
        JsonRpcDispatcher dispatcher = PureJavaDemoApplication.createDispatcher(
            JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);

        JsonNode response = parse(PureJavaDemoApplication.handle(dispatcher, """
            {"jsonrpc":"2.0","method":"ping","id":1}
            """));

        assertEquals("pong", response.get("result").asString());
        assertEquals(1, response.get("id").asInt());
    }

    @Test
    void returnsNoBodyForNotificationRequest() throws JacksonException {
        JsonRpcDispatcher dispatcher = PureJavaDemoApplication.createDispatcher(
            JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);

        String responseBody = PureJavaDemoApplication.handle(dispatcher, """
            {"jsonrpc":"2.0","method":"ping"}
            """);

        assertTrue(responseBody.isEmpty());
    }

    @Test
    void returnsBatchWithOnlyNonNotificationResponses() throws JacksonException {
        JsonRpcDispatcher dispatcher = PureJavaDemoApplication.createDispatcher(
            JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);

        JsonNode batch = parse(PureJavaDemoApplication.handle(dispatcher, """
            [
              {"jsonrpc":"2.0","method":"typed.upper","params":{"value":"demo"},"id":1},
              {"jsonrpc":"2.0","method":"typed.tags"},
              {"jsonrpc":"2.0","method":"missing","id":2}
            ]
            """));

        assertTrue(batch.isArray());
        assertEquals(2, batch.size());
        assertEquals("DEMO", batch.get(0).get("result").get("value").asString());
        assertEquals(-32601, batch.get(1).get("error").get("code").asInt());
    }

    @Test
    void returnsParseErrorForInvalidJson() throws JacksonException {
        JsonRpcDispatcher dispatcher = PureJavaDemoApplication.createDispatcher(
            JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);

        JsonNode response = parse(PureJavaDemoApplication.handle(dispatcher, "{"));

        assertEquals(-32700, response.get("error").get("code").asInt());
        assertTrue(response.get("id").isNull());
    }

    @Test
    void appliesConfigurableParamsTypeViolationCodePolicy() throws JacksonException {
        JsonRpcDispatcher strict = PureJavaDemoApplication.createDispatcher(
            JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST);

        JsonNode response = parse(PureJavaDemoApplication.handle(strict, """
            {"jsonrpc":"2.0","method":"typed.upper","params":"invalid-shape","id":9}
            """));

        assertEquals(-32600, response.get("error").get("code").asInt());
    }

    private JsonNode parse(String json) throws JacksonException {
        return OBJECT_MAPPER.readTree(json);
    }
}
