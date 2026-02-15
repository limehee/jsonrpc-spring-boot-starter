package com.limehee.jsonrpc.spring.boot.autoconfigure;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = JsonRpcRegistrationStylesE2ETest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jsonrpc.enabled=true",
                "jsonrpc.path=/jsonrpc",
                "jsonrpc.scan-annotated-methods=true"
        }
)
class JsonRpcRegistrationStylesE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void e2eSupportsAnnotationWithRecordReturn() throws Exception {
        JsonNode body = call("""
                {"jsonrpc":"2.0","method":"annot.user","params":{"id":11},"id":1}
                """);

        assertEquals(11, body.get("result").get("id").asInt());
        assertEquals("user-11", body.get("result").get("name").asText());
    }

    @Test
    void e2eSupportsManualRegistration() throws Exception {
        JsonNode body = call("""
                {"jsonrpc":"2.0","method":"manual.ping","id":2}
                """);

        assertEquals("pong-manual", body.get("result").asText());
    }

    @Test
    void e2eSupportsTypedFactoryRegistrationWithClassParamAndCollectionReturn() throws Exception {
        JsonNode upper = call("""
                {"jsonrpc":"2.0","method":"typed.upper","params":{"value":"rpc"},"id":3}
                """);
        JsonNode tags = call("""
                {"jsonrpc":"2.0","method":"typed.tags","id":4}
                """);

        assertEquals("RPC", upper.get("result").get("result").asText());
        assertTrue(tags.get("result").isArray());
        assertEquals("alpha", tags.get("result").get(0).asText());
        assertEquals("beta", tags.get("result").get(1).asText());
    }

    private JsonNode call(String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/jsonrpc"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return OBJECT_MAPPER.readTree(response.body());
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
            return JsonRpcMethodRegistration.of("manual.ping", params -> StringNode.valueOf("pong-manual"));
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
    }

    static class UserRequest {
        public int id;
    }

    record UserResponse(int id, String name) {
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
