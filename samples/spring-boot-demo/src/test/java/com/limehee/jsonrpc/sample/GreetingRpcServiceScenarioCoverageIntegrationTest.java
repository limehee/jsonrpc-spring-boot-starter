package com.limehee.jsonrpc.sample;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GreetingRpcServiceScenarioCoverageIntegrationTest extends AbstractJsonRpcIntegrationSupport {

    @Test
    void supportsManualRegistrationScenario() throws Exception {
        JsonNode body = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"manual.echo","id":31}
            """);

        assertEquals("echo", body.get("result").asString());
        assertEquals(31, body.get("id").asInt());
    }

    @Test
    void supportsTypedRegistrationScenarios() throws Exception {
        JsonNode upper = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"typed.upper","params":{"value":"spring"},"id":32}
            """);
        JsonNode tags = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"typed.tags","id":33}
            """);

        assertEquals("SPRING", upper.get("result").get("value").asString());
        assertTrue(tags.get("result").isArray());
        assertEquals("alpha", tags.get("result").get(0).asString());
        assertEquals("beta", tags.get("result").get(1).asString());
    }

    @Test
    void supportsPositionalParamsAndMixedBatchFlow() throws Exception {
        JsonNode sum = invokeJsonRpc("""
            {"jsonrpc":"2.0","method":"sum","params":[4,5],"id":34}
            """);
        JsonNode batch = invokeJsonRpc("""
            [
              {"jsonrpc":"2.0","method":"manual.echo","id":35},
              {"jsonrpc":"2.0","method":"typed.tags"},
              {"jsonrpc":"2.0","method":"missing","id":36}
            ]
            """);

        assertEquals(9, sum.get("result").asInt());
        assertTrue(batch.isArray());
        assertEquals(2, batch.size());
        assertEquals("echo", batch.get(0).get("result").asString());
        assertEquals(-32601, batch.get(1).get("error").get("code").asInt());
    }
}
