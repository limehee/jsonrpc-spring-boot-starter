# Pure Java Guide

`jsonrpc-core` is transport-agnostic and can be used without Spring. This guide shows how to use it in plain Java applications, custom servers, workers, and tests.

## 1. Dependency

Maven:

```xml
<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-core</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.limehee:jsonrpc-core:0.1.0")
```

Gradle (Groovy DSL):

```groovy
implementation 'io.github.limehee:jsonrpc-core:0.1.0'
```

## 2. Minimal Dispatcher

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.StringNode;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;

ObjectMapper mapper = JsonMapper.builder().build();
JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

dispatcher.register("ping", params -> StringNode.valueOf("pong"));

JsonNode request = mapper.readTree("""
{"jsonrpc":"2.0","method":"ping","id":1}
""");

JsonRpcDispatchResult result = dispatcher.dispatch(request);
String json = mapper.writeValueAsString(result.singleResponse().orElseThrow());
System.out.println(json);
```

## 3. Typed Registration (`JsonRpcTypedMethodHandlerFactory`)

```java
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.limehee.jsonrpc.core.DefaultJsonRpcTypedMethodHandlerFactory;
import com.limehee.jsonrpc.core.JacksonJsonRpcParameterBinder;
import com.limehee.jsonrpc.core.JacksonJsonRpcResultWriter;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;

record UpperIn(String value) {}
record UpperOut(String value) {}

ObjectMapper mapper = JsonMapper.builder().build();
JsonRpcTypedMethodHandlerFactory factory = new DefaultJsonRpcTypedMethodHandlerFactory(
        new JacksonJsonRpcParameterBinder(mapper),
        new JacksonJsonRpcResultWriter(mapper)
);

JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

dispatcher.register(
        "typed.upper",
        factory.unary(UpperIn.class, in -> new UpperOut(in.value().toUpperCase()))
);
```

## 4. DTO Shapes: Record, Class, Collection, Map

### 4.1 Record input/output

```java
record UserQuery(long id) {}
record UserView(long id, String name) {}
```

### 4.2 POJO input

```java
class CreateTagRequest {
    public String name;
}
```

### 4.3 Collection return

```java
dispatcher.register(
        "tags.list",
        factory.noParams(() -> List.of("alpha", "beta", "gamma"))
);
```

### 4.4 Map return

```java
dispatcher.register(
        "health",
        factory.noParams(() -> Map.of("status", "UP", "version", "0.1.0"))
);
```

All mapping is Jackson-based via binder/result-writer components.

## 5. Batch, Notification, and Error Cases

### 5.1 Batch request

```json
[
  {"jsonrpc":"2.0","method":"ping","id":1},
  {"jsonrpc":"2.0","method":"ping"},
  {"jsonrpc":"2.0","method":"unknown","id":2}
]
```

Behavior:

- notification entry (no `id`) is executed and omitted from responses
- unknown method becomes `-32601`
- response array contains only non-notification entries in traversal order

### 5.2 Empty batch

`[]` returns one `-32600 Invalid Request` error object.

### 5.3 Parse/validation/runtime errors

- invalid JSON text -> `-32700`
- invalid request shape -> `-32600`
- parameter mapping failure -> `-32602`
- unhandled runtime exception -> `-32603`

## 6. Compose Custom Dispatcher Pipeline

You can inject your own implementations for parser/validator/invoker/etc.

```java
import com.limehee.jsonrpc.core.DefaultJsonRpcExceptionResolver;
import com.limehee.jsonrpc.core.DefaultJsonRpcMethodInvoker;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestParser;
import com.limehee.jsonrpc.core.DefaultJsonRpcRequestValidator;
import com.limehee.jsonrpc.core.DefaultJsonRpcResponseComposer;
import com.limehee.jsonrpc.core.DirectJsonRpcNotificationExecutor;
import com.limehee.jsonrpc.core.InMemoryJsonRpcMethodRegistry;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistrationConflictPolicy;
import java.util.List;

JsonRpcDispatcher dispatcher = new JsonRpcDispatcher(
        new InMemoryJsonRpcMethodRegistry(JsonRpcMethodRegistrationConflictPolicy.REJECT),
        new DefaultJsonRpcRequestParser(),
        new DefaultJsonRpcRequestValidator(),
        new DefaultJsonRpcMethodInvoker(),
        new DefaultJsonRpcExceptionResolver(false),
        new DefaultJsonRpcResponseComposer(),
        100,
        List.of(),
        new DirectJsonRpcNotificationExecutor()
);
```

This keeps protocol behavior while letting you customize policy and implementation.

## 7. Custom Transport Pattern

When using Netty, Undertow, Vert.x, CLI stdin/stdout, message queues, or any custom transport, use this pattern:

1. Parse bytes/string into `JsonNode` with Jackson.
2. On parse failure, return `dispatcher.parseErrorResponse()` equivalent payload.
3. Call `dispatcher.dispatch(payload)`.
4. If `hasResponse()` is false, do not emit body.
5. If response exists, serialize single response or response list to JSON.

## 8. Concurrency Notes

- `JsonRpcDispatcher` invocation path is stateless per request except method registry lookups.
- Notification behavior depends on the configured `JsonRpcNotificationExecutor`.
- For asynchronous notification isolation in plain Java, provide an executor-backed implementation.

## 9. Deep References

- Protocol matrix: [`protocol-and-compliance.md`](protocol-and-compliance.md)
- Registration/binding semantics: [`registration-and-binding.md`](registration-and-binding.md)
- Extension interfaces: [`extension-points.md`](extension-points.md)
