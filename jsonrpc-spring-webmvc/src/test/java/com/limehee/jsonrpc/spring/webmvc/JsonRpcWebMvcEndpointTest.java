package com.limehee.jsonrpc.spring.webmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcErrorCode;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;

class JsonRpcWebMvcEndpointTest {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));

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

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void returnsParseErrorForEmptyBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new byte[0]))
            .andExpect(status().isOk())
            .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void returnsParseErrorForWhitespaceOnlyBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("   "))
            .andExpect(status().isOk())
            .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void returnsSingleSuccessResponseForRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
        assertEquals("pong", response.result().asString());
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
        assertEquals("pong", response.get(0).get("result").asString());
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND, response.get(1).get("error").get("code").asInt());
    }

    @Test
    void returnsInvalidRequestWhenPayloadTooLarge() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
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

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
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

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
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
    void allowsDuplicateRequestMembersWhenDuplicateRejectionIsDisabled() throws Exception {
        MvcResult result = mockMvc.perform(post("/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1,\"id\":2}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
        assertEquals("pong", response.result().asString());
        assertEquals(2, response.id().asInt());
    }

    @Test
    void rejectsDuplicateRequestMembersWhenDuplicateRejectionIsEnabled() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
        JsonRpcWebMvcEndpoint endpoint = new JsonRpcWebMvcEndpoint(
            dispatcher,
            OBJECT_MAPPER,
            new DefaultJsonRpcHttpStatusStrategy(),
            1024 * 1024,
            JsonRpcWebMvcObserver.noOp(),
            true
        );
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        MvcResult result = localMockMvc.perform(post("/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1,\"id\":2}"))
            .andExpect(status().isOk())
            .andReturn();

        JsonRpcResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsByteArray(),
            JsonRpcResponse.class);
        assertEquals(JsonRpcErrorCode.PARSE_ERROR, response.error().code());
    }

    @Test
    void usesCustomStatusStrategyForParseErrorAndPayloadLimit() throws Exception {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
        JsonRpcHttpStatusStrategy strategy = new JsonRpcHttpStatusStrategy() {
            @Override
            public HttpStatus statusForSingle(JsonRpcResponse response) {
                return HttpStatus.OK;
            }

            @Override
            public HttpStatus statusForBatch(List<JsonRpcResponse> responses) {
                return HttpStatus.OK;
            }

            @Override
            public HttpStatus statusForNotificationOnly() {
                return HttpStatus.NO_CONTENT;
            }

            @Override
            public HttpStatus statusForParseError() {
                return HttpStatus.BAD_REQUEST;
            }

            @Override
            public HttpStatus statusForRequestTooLarge() {
                return HttpStatus.valueOf(413);
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
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
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
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
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

    @Test
    void constructorRejectsNonPositiveMaxRequestBytes() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

        assertThrows(IllegalArgumentException.class, () -> new JsonRpcWebMvcEndpoint(
            dispatcher,
            OBJECT_MAPPER,
            new DefaultJsonRpcHttpStatusStrategy(),
            0
        ));

        assertThrows(IllegalArgumentException.class, () -> new JsonRpcWebMvcEndpoint(
            dispatcher,
            OBJECT_MAPPER,
            new DefaultJsonRpcHttpStatusStrategy(),
            -1
        ));
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

    @Test
    void rejectsDeclaredContentLengthAboveLimitWithoutReadingBody() throws Exception {
        List<Integer> observed = new ArrayList<>();
        JsonRpcWebMvcEndpoint endpoint = limitedEndpoint(16, observed);
        CountingInputStream body = new CountingInputStream(new byte[1024]);

        var response = endpoint.handle(body, 1024L);

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, errorCode(response.getBody()));
        assertEquals(0, body.bytesRead());
        assertEquals(List.of(1024), observed);
    }

    @Test
    void stopsReadingUndeclaredLengthBodyOnceLimitIsExceeded() throws Exception {
        List<Integer> observed = new ArrayList<>();
        JsonRpcWebMvcEndpoint endpoint = limitedEndpoint(16, observed);
        CountingInputStream body = new CountingInputStream(new byte[1024 * 1024]);

        var response = endpoint.handle(body, null);

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, errorCode(response.getBody()));
        assertEquals(17, body.bytesRead());
        assertEquals(List.of(17), observed);
    }

    @Test
    void stopsReadingWhenDeclaredContentLengthUnderstatesActualBody() throws Exception {
        JsonRpcWebMvcEndpoint endpoint = limitedEndpoint(16, new ArrayList<>());
        CountingInputStream body = new CountingInputStream(new byte[1024 * 1024]);

        var response = endpoint.handle(body, 4L);

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, errorCode(response.getBody()));
        assertEquals(17, body.bytesRead());
    }

    @Test
    void acceptsPayloadExactlyAtLimit() throws Exception {
        byte[] payload = "{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}".getBytes(StandardCharsets.UTF_8);
        JsonRpcWebMvcEndpoint endpoint = limitedEndpoint(payload.length, new ArrayList<>());

        var response = endpoint.handle(new ByteArrayInputStream(payload), (long) payload.length);

        JsonRpcResponse rpcResponse = OBJECT_MAPPER.readValue(response.getBody(), JsonRpcResponse.class);
        assertEquals("pong", rpcResponse.result().asString());
    }

    @Test
    void rejectsChunkedPayloadAboveLimitOverHttp() throws Exception {
        JsonRpcWebMvcEndpoint endpoint = limitedEndpoint(8, new ArrayList<>());
        MockMvc localMockMvc = MockMvcBuilders.standaloneSetup(endpoint).build();

        MvcResult result = localMockMvc.perform(post("/jsonrpc")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Transfer-Encoding", "chunked")
                .content("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(JsonRpcErrorCode.INVALID_REQUEST, errorCode(result.getResponse().getContentAsString()));
    }

    private static JsonRpcWebMvcEndpoint limitedEndpoint(int maxRequestBytes, List<Integer> observedSizes) {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
        return new JsonRpcWebMvcEndpoint(
            dispatcher,
            OBJECT_MAPPER,
            new DefaultJsonRpcHttpStatusStrategy(),
            maxRequestBytes,
            new JsonRpcWebMvcObserver() {
                @Override
                public void onRequestTooLarge(int actualBytes, int maxBytes) {
                    observedSizes.add(actualBytes);
                }
            }
        );
    }

    private static int errorCode(String body) {
        return OBJECT_MAPPER.readValue(body, JsonRpcResponse.class).error().code();
    }

    private static final class CountingInputStream extends InputStream {

        private final byte[] data;
        private int position;

        private CountingInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return position < data.length ? data[position++] & 0xFF : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (position >= data.length) {
                return -1;
            }
            int count = Math.min(length, data.length - position);
            System.arraycopy(data, position, buffer, offset, count);
            position += count;
            return count;
        }

        private int bytesRead() {
            return position;
        }
    }
}
