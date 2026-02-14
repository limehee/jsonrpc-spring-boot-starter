package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limehee.jsonrpc.core.JsonRpcMethod;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = JsonRpcLibraryE2ETest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jsonrpc.enabled=true",
                "jsonrpc.path=/jsonrpc",
                "jsonrpc.scan-annotated-methods=true"
        }
)
class JsonRpcLibraryE2ETest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void e2eHttpRequestReturnsJsonRpcSuccessResponse() throws Exception {
        HttpResponse<String> response = call("""
                {"jsonrpc":"2.0","method":"ping","id":10}
                """);

        assertEquals(200, response.statusCode());
        JsonNode body = OBJECT_MAPPER.readTree(response.body());
        assertEquals("2.0", body.get("jsonrpc").asText());
        assertEquals(10, body.get("id").asInt());
        assertEquals("pong", body.get("result").asText());
    }

    @Test
    void e2eNotificationReturnsNoContent() throws Exception {
        HttpResponse<String> response = call("""
                {"jsonrpc":"2.0","method":"ping"}
                """);

        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty());
    }

    @Test
    void e2eUnknownMethodReturnsJsonRpcError() throws Exception {
        HttpResponse<String> response = call("""
                {"jsonrpc":"2.0","method":"missing","id":99}
                """);

        assertEquals(200, response.statusCode());
        JsonNode body = OBJECT_MAPPER.readTree(response.body());
        assertEquals(99, body.get("id").asInt());
        assertEquals(-32601, body.get("error").get("code").asInt());
    }

    private HttpResponse<String> call(String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/jsonrpc"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestRpcConfiguration.class)
    static class TestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class TestRpcConfiguration {
        @Bean
        E2eRpcService e2eRpcService() {
            return new E2eRpcService();
        }
    }

    static class E2eRpcService {
        @JsonRpcMethod("ping")
        public String ping() {
            return "pong";
        }
    }
}
