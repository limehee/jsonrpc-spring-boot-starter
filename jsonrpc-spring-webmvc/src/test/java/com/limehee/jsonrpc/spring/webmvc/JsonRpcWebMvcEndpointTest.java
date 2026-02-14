package com.limehee.jsonrpc.spring.webmvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JsonRpcWebMvcEndpointTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));

        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
                dispatcher,
                OBJECT_MAPPER,
                new DefaultJsonRpcHttpStatusStrategy(),
                1024 * 1024
        );

        mockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();
    }

    @Test
    void returnsParseErrorForInvalidJson() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isOk())
                .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(), JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void returnsParseErrorForEmptyBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[0]))
                .andExpect(status().isOk())
                .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(), JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void returnsParseErrorForWhitespaceOnlyBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("   "))
                .andExpect(status().isOk())
                .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(), JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void returnsSingleSuccessResponseForRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(), JsonRpcResponse.class);
        assertEquals("pong", response.result().asText());
        assertEquals(1, response.id().asInt());
    }

    @Test
    void returnsNoContentForNotification() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}"))
                .andExpect(status().isNoContent());
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
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsBatchResponseWithoutNotifications() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"jsonrpc":"2.0","method":"ping","id":1},
                                  {"jsonrpc":"2.0","method":"ping"},
                                  {"jsonrpc":"2.0","method":"missing","id":2}
                                ]
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = OBJECT_MAPPER.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(response.isArray());
        assertEquals(2, response.size());
        assertEquals("pong", response.get(0).get("result").asText());
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, response.get(1).get("error").get("code").asInt());
    }

    @Test
    void returnsInvalidRequestWhenPayloadTooLarge() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));
        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
                dispatcher,
                OBJECT_MAPPER,
                new DefaultJsonRpcHttpStatusStrategy(),
                8
        );
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        MvcResult result = localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(), JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
    }

    @Test
    void returnsInvalidRequestWhenWhitespacePayloadExceedsLimit() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
                dispatcher,
                OBJECT_MAPPER,
                new DefaultJsonRpcHttpStatusStrategy(),
                2
        );
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        MvcResult result = localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("   "))
                .andExpect(status().isOk())
                .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(), JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, response.error().code());
    }

    @Test
    void rejectsNonJsonContentType() throws Exception {
        mockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void usesCustomStatusStrategyForParseErrorAndPayloadLimit() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));
        JsonRpcHttpStatusStrategy strategy = new JsonRpcHttpStatusStrategy() {
            @Override
            public org.springframework.http.HttpStatus statusForSingle(JsonRpcResponse response) {
                return org.springframework.http.HttpStatus.OK;
            }

            @Override
            public org.springframework.http.HttpStatus statusForBatch(java.util.List<JsonRpcResponse> responses) {
                return org.springframework.http.HttpStatus.OK;
            }

            @Override
            public org.springframework.http.HttpStatus statusForNotificationOnly() {
                return org.springframework.http.HttpStatus.NO_CONTENT;
            }

            @Override
            public org.springframework.http.HttpStatus statusForParseError() {
                return org.springframework.http.HttpStatus.BAD_REQUEST;
            }

            @Override
            public org.springframework.http.HttpStatus statusForRequestTooLarge() {
                return org.springframework.http.HttpStatus.valueOf(413);
            }
        };

        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
                dispatcher,
                OBJECT_MAPPER,
                strategy,
                8
        );
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());

        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                .andExpect(result -> assertEquals(413, result.getResponse().getStatus()));
    }

    @Test
    void notifiesObserverForParseErrorsRequestTooLargeAndNotificationOnly() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));
        RecordingObserver observer = new RecordingObserver();
        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
                dispatcher,
                OBJECT_MAPPER,
                new DefaultJsonRpcHttpStatusStrategy(),
                64,
                observer
        );
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isOk());
        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc":"2.0","method":"ping","params":{"value":"abcdefghijklmnopqrstuvwxyz"},"id":1}
                                """))
                .andExpect(status().isOk());
        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\"}"))
                .andExpect(status().isNoContent());

        assertEquals(1, observer.parseErrors);
        assertEquals(1, observer.requestTooLarge);
        assertEquals(1, observer.notificationOnly);
        assertEquals(1, observer.notificationOnlyRequestCount);
    }

    @Test
    void notifiesObserverForSingleAndBatchResponses() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));
        RecordingObserver observer = new RecordingObserver();
        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
                dispatcher,
                OBJECT_MAPPER,
                new DefaultJsonRpcHttpStatusStrategy(),
                1024 * 1024,
                observer
        );
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
                .andExpect(status().isOk());
        localMockMvc.perform(post("/jsonrpc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"jsonrpc":"2.0","method":"ping","id":1},
                                  {"jsonrpc":"2.0","method":"ping"},
                                  {"jsonrpc":"2.0","method":"missing","id":2}
                                ]
                                """))
                .andExpect(status().isOk());

        assertEquals(1, observer.singleResponses);
        assertEquals(1, observer.batchResponses);
        assertEquals(3, observer.lastBatchRequestCount);
        assertEquals(2, observer.lastBatchResponseCount);
    }

    private static final class RecordingObserver implements JsonRpcWebMvcObserver {
        int parseErrors;
        int requestTooLarge;
        int notificationOnly;
        int notificationOnlyRequestCount;
        int singleResponses;
        int batchResponses;
        int lastBatchRequestCount;
        int lastBatchResponseCount;

        @Override
        public void onParseError() {
            parseErrors++;
        }

        @Override
        public void onRequestTooLarge(int actualBytes, int maxBytes) {
            requestTooLarge++;
        }

        @Override
        public void onSingleResponse(JsonRpcResponse response) {
            singleResponses++;
        }

        @Override
        public void onBatchResponse(int requestCount, List<JsonRpcResponse> responses) {
            batchResponses++;
            lastBatchRequestCount = requestCount;
            lastBatchResponseCount = responses.size();
        }

        @Override
        public void onNotificationOnly(boolean batch, int requestCount) {
            notificationOnly++;
            notificationOnlyRequestCount = requestCount;
        }
    }
}
