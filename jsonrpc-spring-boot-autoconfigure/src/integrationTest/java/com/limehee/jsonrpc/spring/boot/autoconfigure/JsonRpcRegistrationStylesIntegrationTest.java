package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = JsonRpcRegistrationStylesIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "jsonrpc.enabled=true",
                "jsonrpc.path=/jsonrpc",
                "jsonrpc.scan-annotated-methods=true"
        }
)
class JsonRpcRegistrationStylesIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void supportsAnnotationRegistrationWithClassParamAndRecordReturn() throws Exception {
        JsonNode body = invoke("""
                {"jsonrpc":"2.0","method":"annot.user","params":{"id":7},"id":1}
                """);

        assertEquals(7, body.get("result").get("id").asInt());
        assertEquals("user-7", body.get("result").get("name").asText());
    }

    @Test
    void supportsAnnotationRegistrationWithClassParamAndCollectionReturn() throws Exception {
        JsonNode body = invoke("""
                {"jsonrpc":"2.0","method":"annot.range","params":{"start":2,"end":4},"id":2}
                """);

        assertTrue(body.get("result").isArray());
        assertEquals(3, body.get("result").size());
        assertEquals(2, body.get("result").get(0).asInt());
        assertEquals(4, body.get("result").get(2).asInt());
    }

    @Test
    void supportsManualJsonRpcMethodRegistration() throws Exception {
        JsonNode body = invoke("""
                {"jsonrpc":"2.0","method":"manual.ping","id":3}
                """);

        assertEquals("pong-manual", body.get("result").asText());
    }

    @Test
    void supportsTypedFactoryRegistrationWithClassParamAndClassReturn() throws Exception {
        JsonNode body = invoke("""
                {"jsonrpc":"2.0","method":"typed.upper","params":{"value":"codex"},"id":4}
                """);

        assertEquals("CODEX", body.get("result").get("result").asText());
    }

    @Test
    void supportsTypedFactoryNoParamsReturningCollection() throws Exception {
        JsonNode body = invoke("""
                {"jsonrpc":"2.0","method":"typed.tags","id":5}
                """);

        assertTrue(body.get("result").isArray());
        assertEquals("alpha", body.get("result").get(0).asText());
        assertEquals("beta", body.get("result").get(1).asText());
    }

    private JsonNode invoke(String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestRpcConfiguration.class)
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRpcConfiguration {
        @Bean
        RegistrationStyleAnnotatedService registrationStyleAnnotatedService() {
            return new RegistrationStyleAnnotatedService();
        }

        @Bean
        JsonRpcMethodRegistration manualPingRegistration() {
            return JsonRpcMethodRegistration.of("manual.ping", params -> TextNode.valueOf("pong-manual"));
        }

        @Bean
        JsonRpcMethodRegistration typedUpperRegistration(JsonRpcTypedMethodHandlerFactory factory) {
            return JsonRpcMethodRegistration.of("typed.upper",
                    factory.unary(UpperInput.class, params -> new UpperOutput(
                            params.value == null ? "" : params.value.toUpperCase())));
        }

        @Bean
        JsonRpcMethodRegistration typedTagsRegistration(JsonRpcTypedMethodHandlerFactory factory) {
            return JsonRpcMethodRegistration.of("typed.tags",
                    factory.noParams(() -> List.of("alpha", "beta")));
        }
    }

    static class RegistrationStyleAnnotatedService {
        @JsonRpcMethod("annot.user")
        public UserResponse user(UserRequest request) {
            return new UserResponse(request.id, "user-" + request.id);
        }

        @JsonRpcMethod("annot.range")
        public List<Integer> range(RangeRequest request) {
            List<Integer> values = new ArrayList<>();
            for (int i = request.start; i <= request.end; i++) {
                values.add(i);
            }
            return values;
        }
    }

    static class UserRequest {
        public int id;
    }

    record UserResponse(int id, String name) {
    }

    static class RangeRequest {
        public int start;
        public int end;
    }

    static class UpperInput {
        public String value;
    }

    static class UpperOutput {
        public String result;

        UpperOutput(String result) {
            this.result = result;
        }
    }
}
