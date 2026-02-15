package com.limehee.jsonrpc.core;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRpcPureJavaE2ETest {

    private PureJavaJsonRpcServer server;

    @BeforeEach
    void setUp() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        JsonRpcTypedMethodHandlerFactory typedFactory = new DefaultJsonRpcTypedMethodHandlerFactory(
                new JacksonJsonRpcParameterBinder(PureJavaJsonRpcServer.OBJECT_MAPPER),
                new JacksonJsonRpcResultWriter(PureJavaJsonRpcServer.OBJECT_MAPPER)
        );

        dispatcher.register("manual.ping", params -> StringNode.valueOf("pong"));
        dispatcher.register("typed.upper", typedFactory.unary(UpperInput.class,
                input -> new UpperOutput(input.value == null ? "" : input.value.toUpperCase())));
        dispatcher.register("typed.tags", typedFactory.noParams(() -> List.of("alpha", "beta")));

        server = new PureJavaJsonRpcServer(dispatcher);
    }

    @Test
    void e2eReturnsSuccessJsonForManualAndTypedMethods() throws Exception {
        JsonNode ping = parse(server.handle("""
                {"jsonrpc":"2.0","method":"manual.ping","id":1}
                """));
        JsonNode upper = parse(server.handle("""
                {"jsonrpc":"2.0","method":"typed.upper","params":{"value":"core"},"id":2}
                """));

        assertEquals("pong", ping.get("result").asText());
        assertEquals("CORE", upper.get("result").get("value").asText());
    }

    @Test
    void e2eReturnsNoBodyForNotification() throws Exception {
        String body = server.handle("""
                {"jsonrpc":"2.0","method":"manual.ping"}
                """);
        assertTrue(body.isEmpty());
    }

    @Test
    void e2eHandlesBatchAndParseError() throws Exception {
        JsonNode batch = parse(server.handle("""
                [
                  {"jsonrpc":"2.0","method":"typed.tags","id":3},
                  {"jsonrpc":"2.0","method":"missing","id":4}
                ]
                """));
        JsonNode parseError = parse(server.handle("{"));

        assertTrue(batch.isArray());
        assertEquals(2, batch.size());
        assertEquals("alpha", batch.get(0).get("result").get(0).asText());
        assertEquals(-32601, batch.get(1).get("error").get("code").asInt());
        assertEquals(-32700, parseError.get("error").get("code").asInt());
    }

    private JsonNode parse(String json) throws JacksonException {
        return PureJavaJsonRpcServer.OBJECT_MAPPER.readTree(json);
    }

    static class UpperInput {
        public String value;
    }

    static class UpperOutput {
        public String value;

        UpperOutput(String value) {
            this.value = value;
        }
    }

    static class PureJavaJsonRpcServer {
        private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

        private final JsonRpcDispatcher dispatcher;

        PureJavaJsonRpcServer(JsonRpcDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        String handle(String rawJson) throws JacksonException {
            JsonRpcDispatchResult dispatchResult;
            try {
                JsonNode payload = OBJECT_MAPPER.readTree(rawJson);
                dispatchResult = dispatcher.dispatch(payload);
            } catch (JacksonException ex) {
                return OBJECT_MAPPER.writeValueAsString(dispatcher.parseErrorResponse());
            }

            if (!dispatchResult.hasResponse()) {
                return "";
            }
            if (dispatchResult.isBatch()) {
                return OBJECT_MAPPER.writeValueAsString(dispatchResult.responses());
            }
            return OBJECT_MAPPER.writeValueAsString(dispatchResult.singleResponse().orElseThrow());
        }
    }
}
