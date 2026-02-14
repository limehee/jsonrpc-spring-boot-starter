package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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

@SpringBootTest(
        classes = JsonRpcLibraryIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "jsonrpc.enabled=true",
                "jsonrpc.path=/jsonrpc",
                "jsonrpc.scan-annotated-methods=true",
                "jsonrpc.max-request-bytes=64"
        }
)
class JsonRpcLibraryIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private JsonRpcDispatcher dispatcher;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void integrationWiresDispatcherAndAnnotatedMethodRegistration() throws Exception {
        JsonRpcDispatchResult result = dispatcher.dispatch(OBJECT_MAPPER.readTree("""
                {"jsonrpc":"2.0","method":"echo","params":{"value":"x"},"id":1}
                """));

        JsonRpcResponse response = result.singleResponse().orElseThrow();
        assertEquals("echo:x", response.result().asText());
    }

    @Test
    void integrationInvokesEndpointAndReturnsJsonRpcPayload() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"ping","id":2}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
        assertEquals("2.0", body.get("jsonrpc").asText());
        assertEquals(2, body.get("id").asInt());
        assertEquals("pong", body.get("result").asText());
    }

    @Test
    void integrationReturnsNoContentForNotification() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"ping"}
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void integrationRespectsRequestSizeLimit() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"echo","params":{"value":"abcdefghijklmnopqrstuvwxyz"},"id":3}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
        assertNotNull(body.get("error"));
        assertEquals(-32600, body.get("error").get("code").asInt());
        assertTrue(body.get("id").isNull());
        assertFalse(body.has("result"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestRpcConfiguration.class)
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRpcConfiguration {
        @Bean
        IntegrationRpcService integrationRpcService() {
            return new IntegrationRpcService();
        }
    }

    static class IntegrationRpcService {
        @JsonRpcMethod("ping")
        public String ping() {
            return "pong";
        }

        @JsonRpcMethod("echo")
        public String echo(EchoParams params) {
            return "echo:" + params.value();
        }

        record EchoParams(String value) {
        }
    }
}
