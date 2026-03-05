package com.limehee.jsonrpc.sample.purejava;

import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseValidator;
import com.limehee.jsonrpc.core.DirectJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponse;
import com.limehee.jsonrpc.core.JsonRpcIncomingResponseEnvelope;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import com.limehee.jsonrpc.core.JsonRpcParamsTypeViolationCodePolicy;
import com.limehee.jsonrpc.core.JsonRpcRequestValidationOptions;
import com.limehee.jsonrpc.core.JsonRpcResponseErrorCodePolicy;
import com.limehee.jsonrpc.core.JsonRpcResponseValidationOptions;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;

public final class ValidationProfileExample {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private ValidationProfileExample() {
    }

    public static JsonRpcRequestValidationOptions strictRequestOptions() {
        return JsonRpcRequestValidationOptions.builder()
            .requireJsonRpcVersion20(true)
            .requireIdMember(true)
            .allowNullId(false)
            .allowStringId(true)
            .allowNumericId(true)
            .allowFractionalId(false)
            .rejectResponseFields(true)
            .paramsTypeViolationCodePolicy(JsonRpcParamsTypeViolationCodePolicy.INVALID_REQUEST)
            .build();
    }

    public static JsonRpcResponseValidationOptions strictResponseOptions() {
        return JsonRpcResponseValidationOptions.builder()
            .requireJsonRpcVersion20(true)
            .requireIdMember(true)
            .allowNullId(false)
            .allowStringId(true)
            .allowNumericId(true)
            .allowFractionalId(false)
            .requireExclusiveResultOrError(true)
            .requireErrorObjectWhenPresent(true)
            .requireIntegerErrorCode(true)
            .requireStringErrorMessage(true)
            .rejectRequestFields(true)
            .rejectDuplicateMembers(true)
            .errorCodePolicy(JsonRpcResponseErrorCodePolicy.STANDARD_OR_SERVER_ERROR_RANGE)
            .build();
    }

    public static JsonRpcDispatcher createStrictDispatcher() {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
            new InMemoryJsonRpcMethodRegistry(JsonRpcMethodRegistrationConflictPolicy.REJECT),
            new DefaultJsonRpcRequestParser(),
            new DefaultJsonRpcRequestValidator(strictRequestOptions()),
            new DefaultJsonRpcMethodInvoker(),
            new DefaultJsonRpcExceptionResolver(false),
            new DefaultJsonRpcResponseComposer(),
            100,
            List.of(),
            new DirectJsonRpcNotificationExecutor()
        );
        dispatcher.register("ping", params -> StringNode.valueOf("pong"));
        return dispatcher;
    }

    public static JsonRpcDispatchResult dispatchStrict(String rawRequest) throws JacksonException {
        JsonRpcDispatcher dispatcher = createStrictDispatcher();
        return dispatcher.dispatch(OBJECT_MAPPER.readTree(rawRequest));
    }

    public static List<JsonRpcIncomingResponse> parseAndValidateStrictResponses(String rawPayload) {
        JsonRpcResponseValidationOptions options = strictResponseOptions();
        DefaultJsonRpcResponseParser parser = new DefaultJsonRpcResponseParser(options.rejectDuplicateMembers());
        DefaultJsonRpcResponseValidator validator = new DefaultJsonRpcResponseValidator(options);

        JsonRpcIncomingResponseEnvelope envelope = parser.parse(rawPayload);
        List<JsonRpcIncomingResponse> responses = new ArrayList<>(envelope.responses().size());
        for (JsonRpcIncomingResponse response : envelope.responses()) {
            validator.validate(response);
            responses.add(response);
        }
        return responses;
    }
}
