package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jsonrpc.path=/rpc"
})
class GreetingRpcServiceCustomPathIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void servesJsonRpcOnConfiguredPath() throws Exception {
        JsonNode body = invokeJsonRpc("/rpc", """
                {"jsonrpc":"2.0","method":"ping","id":1}
                """, 200);

        assertEquals("pong", body.get("result").asText());
    }

    @Test
    void defaultPathIsNotMappedWhenCustomPathConfigured() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"ping","id":1}
                                """))
                .andExpect(status().isNotFound());
    }
}
