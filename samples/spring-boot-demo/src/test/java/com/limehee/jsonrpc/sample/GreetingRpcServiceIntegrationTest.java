package com.limehee.jsonrpc.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcException;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(GreetingRpcServiceIntegrationTest.BoomMethodConfig.class)
class GreetingRpcServiceIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Autowired
    private JsonRpcDispatcher dispatcher;

    @Autowired
    private GreetingRpcService greetingRpcService;

    @Test
    void registersGreetingRpcServiceBeanAndAnnotatedMethods() throws Exception {
        assertNotNull(greetingRpcService);

        JsonRpcResponse ping = dispatchSingle("""
                {"jsonrpc":"2.0","method":"ping","id":1}
                """);
        JsonRpcResponse greet = dispatchSingle("""
                {"jsonrpc":"2.0","method":"greet","params":{"name":"codex"},"id":2}
                """);
        JsonRpcResponse sum = dispatchSingle("""
                {"jsonrpc":"2.0","method":"sum","params":{"left":2,"right":3},"id":3}
                """);

        assertEquals("pong", ping.result().asText());
        assertEquals("hello codex", greet.result().asText());
        assertEquals(5, sum.result().asInt());
    }

    @Test
    void returnsExpectedSuccessJsonForPingRequest() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"ping","id":10}
                """);

        assertEquals("2.0", body.get("jsonrpc").asText());
        assertEquals(10, body.get("id").asInt());
        assertEquals("pong", body.get("result").asText());
        assertFalse(body.has("error"));
    }

    @Test
    void bindsObjectAndNamedParamsAndReturnsExpectedJson() throws Exception {
        JsonNode greetBody = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"greet","params":{"name":"spring"},"id":11}
                """);
        JsonNode sumBody = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"sum","params":{"left":7,"right":5},"id":12}
                """);

        assertEquals("hello spring", greetBody.get("result").asText());
        assertEquals(12, sumBody.get("result").asInt());
    }

    @Test
    void returnsJsonRpcErrorForUnknownMethod() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"unknown","id":99}
                """);

        assertEquals("2.0", body.get("jsonrpc").asText());
        assertEquals(99, body.get("id").asInt());
        assertTrue(body.has("error"));
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, body.get("error").get("code").asInt());
        assertFalse(body.has("result"));
    }

    @Test
    void returnsParseErrorForMalformedJson() throws Exception {
        JsonNode body = invokeJsonRpc("/jsonrpc", "{", 200);

        assertEquals("2.0", body.get("jsonrpc").asText());
        assertTrue(body.get("id").isNull());
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, body.get("error").get("code").asInt());
    }

    @Test
    void returnsInvalidRequestForRequestShapeError() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","params":[]}
                """);

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, body.get("error").get("code").asInt());
        assertTrue(body.get("id").isNull());
    }

    @Test
    void returnsInvalidParamsForBindingMismatch() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"sum","params":{"left":2},"id":15}
                """);

        assertEquals(15, body.get("id").asInt());
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS, body.get("error").get("code").asInt());
    }

    @Test
    void returnsNullIdWhenIdTypeIsInvalid() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"ping","id":{"nested":1}}
                """);

        assertTrue(body.get("id").isNull());
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, body.get("error").get("code").asInt());
    }

    @Test
    void returnsBatchResponseForMixedBatch() throws Exception {
        JsonNode body = invokeJsonRpc("""
                [
                  {"jsonrpc":"2.0","method":"ping","id":1},
                  {"jsonrpc":"2.0","method":"ping"},
                  {"jsonrpc":"2.0","method":"unknown","id":2},
                  1
                ]
                """);

        assertTrue(body.isArray());
        assertEquals(3, body.size());
        assertEquals("pong", body.get(0).get("result").asText());
        assertEquals(1, body.get(0).get("id").asInt());
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, body.get(1).get("error").get("code").asInt());
        assertEquals(2, body.get(1).get("id").asInt());
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, body.get(2).get("error").get("code").asInt());
    }

    @Test
    void returnsInvalidRequestForEmptyBatch() throws Exception {
        JsonNode body = invokeJsonRpc("[]");

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, body.get("error").get("code").asInt());
        assertTrue(body.get("id").isNull());
    }

    @Test
    void returnsNoContentForNotificationOnlyBatch() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"jsonrpc":"2.0","method":"ping"},
                                  {"jsonrpc":"2.0","method":"ping"}
                                ]
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void returnsNoContentForNotificationRequest() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"ping"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void rejectsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void hidesErrorDataByDefault() throws Exception {
        JsonNode body = invokeJsonRpc("""
                {"jsonrpc":"2.0","method":"boom","id":70}
                """);

        assertEquals(-32001, body.get("error").get("code").asInt());
        assertNull(body.get("error").get("data"));
    }

    private JsonRpcResponse dispatchSingle(String requestJson) throws Exception {
        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree(requestJson));
        return result.singleResponse().orElseThrow();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BoomMethodConfig {
        @Bean
        JsonRpcMethodRegistration boomMethod() {
            return JsonRpcMethodRegistration.of("boom", params -> {
                throw new JsonRpcException(-32001, "domain", TextNode.valueOf("secret"));
            });
        }
    }
}
