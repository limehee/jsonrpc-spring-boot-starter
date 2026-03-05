package com.limehee.jsonrpc.sample.purejava;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.DefaultJsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.DirectJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JacksonJsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JacksonJsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponse;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import com.limehee.jsonrpc.core.JsonRpcParamsTypeViolationCodePolicy;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import tools.jackson.databind.node.StringNode;

import java.util.List;

public final class PureJavaDemoApplication {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private PureJavaDemoApplication() {
    }

    public static void main(String[] args) throws JacksonException {
        JsonRpcDispatcher dispatcher = createDispatcher(JsonRpcParamsTypeViolationCodePolicy.INVALID_PARAMS);

        print("single success", handle(dispatcher, """
            {"jsonrpc":"2.0","method":"ping","id":1}
            """));
        print("notification", handle(dispatcher, """
            {"jsonrpc":"2.0","method":"ping"}
            """));
        print("mixed batch", handle(dispatcher, """
            [
              {"jsonrpc":"2.0","method":"typed.upper","params":{"value":"core"},"id":2},
              {"jsonrpc":"2.0","method":"typed.tags"},
              {"jsonrpc":"2.0","method":"missing","id":3}
            ]
            """));
        print("parse error", handle(dispatcher, "{"));

        JsonRpcDispatcher strictDispatcher = createDispatcher(JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST);
        print("strict params shape policy", handle(strictDispatcher, """
            {"jsonrpc":"2.0","method":"typed.upper","params":"invalid-shape","id":9}
            """));

        JsonRpcDispatchResult strictRequestResult = ValidationProfileExample.dispatchStrict("""
            {"jsonrpc":"2.0","method":"ping"}
            """);
        print("strict request profile (require-id-member)", OBJECT_MAPPER.writeValueAsString(
            strictRequestResult.singleResponse().orElseThrow()
        ));

        List<JsonRpcIncomingResponse> strictResponses = ValidationProfileExample.parseAndValidateStrictResponses("""
            {"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"server"}}
            """);
        print("strict response profile", OBJECT_MAPPER.writeValueAsString(strictResponses));
    }

    static String handle(JsonRpcDispatcher dispatcher, String rawJson) throws JacksonException {
        try {
            JsonNode payload = OBJECT_MAPPER.readTree(rawJson);
            JsonRpcDispatchResult result = dispatcher.dispatch(payload);
            if (!result.hasResponse()) {
                return "";
            }
            if (result.isBatch()) {
                return OBJECT_MAPPER.writeValueAsString(result.responses());
            }
            return OBJECT_MAPPER.writeValueAsString(result.singleResponse().orElseThrow());
        } catch (JacksonException ex) {
            return OBJECT_MAPPER.writeValueAsString(dispatcher.parseErrorResponse());
        }
    }

    static JsonRpcDispatcher createDispatcher(JsonRpcParamsTypeViolationCodePolicy policy) {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(JsonRpcMethodRegistrationConflictPolicy.REJECT),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(policy),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(false),
            new DefaultJsonRpcResponseComposer(),
            100,
            List.of(),
            new DirectJsonRpcNotificationExecutor()
        );

        JsonRpcTypedMethodHandlerFactory typedFactory = new DefaultJsonRpcTypedMethodHandlerFactory(
            new JacksonJsonRpcParameterBinder(OBJECT_MAPPER),
            new JacksonJsonRpcResultWriter(OBJECT_MAPPER)
        );

        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
        dispatcher.register("typed.upper", typedFactory.unary(UpperInput.class,
            input -> new UpperOutput(input.value() == null ? "" : input.value().toUpperCase())));
        dispatcher.register("typed.tags", typedFactory.noParams(() -> List.of("alpha", "beta")));
        return dispatcher;
    }

    private static void print(String title, String payload) {
        System.out.println("[" + title + "]");
        System.out.println(payload.isEmpty() ? "(no response)" : payload);
        System.out.println();
    }

    public record UpperInput(String value) {

    }

    public record UpperOutput(String value) {

    }
}
