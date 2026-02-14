package com.limehee.jsonrpc.sample;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class GreetingRpcServiceIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockMvc mockMvc;

    @Autowired
    private JsonRpcDispatcher dispatcher;

    @Autowired
    private GreetingRpcService greetingRpcService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

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
    void returnsNoContentForNotificationRequest() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"ping"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private JsonRpcResponse dispatchSingle(String requestJson) throws Exception {
        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree(requestJson));
        return result.singleResponse().orElseThrow();
    }

    private JsonNode invokeJsonRpc(String requestJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
    }
}
