package com.limehee.jsonrpc.spring.boot.autoconfigure;

import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcRequest;
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AutoConfiguration
@EnableConfigurationProperties(JsonRpcProperties.class)
@ConditionalOnClass(JsonRpcDispatcher.class)
public class JsonRpcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonRpcDispatcher jsonRpcDispatcher(ObjectProvider<JsonRpcMethodRegistration> registrations) {
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        registrations.orderedStream().forEach(registration ->
                dispatcher.register(registration.method(), registration.handler()));
        return dispatcher;
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnBean(JsonRpcDispatcher.class)
    @ConditionalOnProperty(prefix = "jsonrpc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JsonRpcHttpController jsonRpcHttpController(JsonRpcDispatcher dispatcher) {
        return new JsonRpcHttpController(dispatcher);
    }

    @RestController
    static class JsonRpcHttpController {

        private final JsonRpcDispatcher dispatcher;

        JsonRpcHttpController(JsonRpcDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @PostMapping("${jsonrpc.path:/jsonrpc}")
        ResponseEntity<JsonRpcResponse> invoke(@RequestBody JsonRpcRequest request) {
            JsonRpcResponse response = dispatcher.dispatch(request);
            JsonNode id = request != null ? request.getId() : null;
            if (id == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(response);
        }
    }
}
