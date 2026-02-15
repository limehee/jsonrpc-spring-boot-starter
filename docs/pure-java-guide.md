# Pure Java Guide

`jsonrpc-core` can be used without Spring. This is useful for custom transports, embedded servers, CLI tools, or tests.

## 1. Dependency

Maven:

```xml
<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-core</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.limehee:jsonrpc-core:0.1.0-SNAPSHOT")
```

## 2. Minimal Dispatcher

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;

ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

dispatcher.register("ping", params -> StringNode.valueOf("pong"));

JsonNode request = mapper.readTree("""
{"jsonrpc":"2.0","method":"ping","id":1}
""");

JsonRpcDispatchResult result = dispatcher.dispatch(request);
System.out.println(mapper.writeValueAsString(result.singleResponse().orElseThrow()));
```

## 3. Typed Handlers

```java
import com.limehee.jsonrpc.core.DefaultJsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.JacksonJsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JacksonJsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;

record Input(String value) {}
record Output(String value) {}

JsonRpcTypedMethodHandlerFactory factory = new DefaultJsonRpcTypedMethodHandlerFactory(
        new JacksonJsonRpcParameterBinder(mapper),
        new JacksonJsonRpcResultWriter(mapper)
);

dispatcher.register(
        "upper",
        factory.unary(Input.class, in -> new Output(in.value().toUpperCase()))
);
```

## 4. Batch, Notification, and Errors

- Batch requests are arrays of request objects.
- Empty batch returns a single `Invalid Request` error.
- Notification request (`id` absent) executes without response payload.
- Parse/validation/method/params/runtime errors map to standard JSON-RPC codes.

See full rules: [`protocol-and-compliance.md`](protocol-and-compliance.md)

## 5. Custom Core Components

You can construct `JsonRpcDispatcher` with custom implementations for:

- parsing
- validation
- invocation
- exception resolution
- response composition
- notification execution
- interceptor chain

This allows protocol-preserving custom behavior without Spring dependencies.

## 6. Recommended Transport Pattern

When building your own transport:

1. Decode incoming bytes to `JsonNode` (Jackson)
2. Handle JSON parse exception as `dispatcher.parseErrorResponse()` equivalent
3. Call `dispatcher.dispatch(payload)`
4. If `hasResponse()` is false, return no content
5. Otherwise encode returned `JsonRpcResponse` list/single to JSON
