package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractJsonRpcIntegrationSupport {

    protected static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    protected JsonNode invokeJsonRpc(String requestJson) throws Exception {
        return invokeJsonRpc("/jsonrpc", requestJson, 200);
    }

    protected JsonNode invokeJsonRpc(String path, String requestJson, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().is(expectedStatus))
                .andReturn();

        byte[] responseBody = result.getResponse().getContentAsByteArray();
        if (responseBody.length == 0) {
            return null;
        }
        return OBJECT_MAPPER.readTree(responseBody);
    }
}
