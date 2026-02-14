package com.limehee.jsonrpc.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRpcPureJavaIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonRpcDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new JsonRpcDispatcher();

        JsonRpcTypedMethodHandlerFactory typedFactory = new DefaultJsonRpcTypedMethodHandlerFactory(
                new JacksonJsonRpcParameterBinder(OBJECT_MAPPER),
                new JacksonJsonRpcResultWriter(OBJECT_MAPPER)
        );

        dispatcher.register("manual.ping", params -> TextNode.valueOf("pong"));
        dispatcher.register("typed.user", typedFactory.unary(UserRequest.class,
                request -> new UserResponse(request.id, "user-" + request.id)));
        dispatcher.register("typed.tags", typedFactory.noParams(() -> List.of("alpha", "beta")));
    }

    @Test
    void supportsManualAndTypedRegistrationsWithoutSpring() throws Exception {
        JsonNode ping = call("""
                {"jsonrpc":"2.0","method":"manual.ping","id":1}
                """);
        JsonNode user = call("""
                {"jsonrpc":"2.0","method":"typed.user","params":{"id":7},"id":2}
                """);

        assertEquals("pong", ping.get("result").asText());
        assertEquals(7, user.get("result").get("id").asInt());
        assertEquals("user-7", user.get("result").get("name").asText());
    }

    @Test
    void supportsClassParamRecordReturnAndCollectionReturn() throws Exception {
        JsonNode tags = call("""
                {"jsonrpc":"2.0","method":"typed.tags","id":3}
                """);

        assertTrue(tags.get("result").isArray());
        assertEquals(2, tags.get("result").size());
        assertEquals("alpha", tags.get("result").get(0).asText());
        assertEquals("beta", tags.get("result").get(1).asText());
    }

    @Test
    void supportsBatchInPureJavaEnvironment() throws Exception {
        JsonNode batchResult = call("""
                [
                  {"jsonrpc":"2.0","method":"manual.ping","id":1},
                  {"jsonrpc":"2.0","method":"manual.ping"},
                  {"jsonrpc":"2.0","method":"missing","id":2}
                ]
                """);

        assertTrue(batchResult.isArray());
        assertEquals(2, batchResult.size());
        assertEquals("pong", batchResult.get(0).get("result").asText());
        assertEquals(-32601, batchResult.get(1).get("error").get("code").asInt());
    }

    private JsonNode call(String json) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree(json);
        JsonRpcDispatchResult result = dispatcher.dispatch(payload);
        if (!result.hasResponse()) {
            return OBJECT_MAPPER.nullNode();
        }
        if (result.isBatch()) {
            return OBJECT_MAPPER.valueToTree(result.responses());
        }
        return OBJECT_MAPPER.valueToTree(result.singleResponse().orElseThrow());
    }

    static class UserRequest {
        public int id;
    }

    record UserResponse(int id, String name) {
    }
}
