# Spring Boot Guide

This guide covers production-style Spring Boot usage, including registration strategies, conflict handling, and runtime customization.

## 1. Dependency

Maven:

```xml
<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.limehee:jsonrpc-spring-boot-starter:0.1.0")
```

Gradle (Groovy DSL):

```groovy
implementation 'io.github.limehee:jsonrpc-spring-boot-starter:0.1.0'
```

## 2. Endpoint Exposure

When `jsonrpc.enabled=true` (default), the starter auto-registers a WebMVC endpoint:

- `POST ${jsonrpc.path}`
- default path: `/jsonrpc`
- content type: `application/json`

Example:

```yaml
jsonrpc:
  enabled: true
  path: /jsonrpc
```

## 3. Registration Styles

### 3.1 Annotation style (`@JsonRpcMethod`)

```java
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcParam;
import org.springframework.stereotype.Service;

@Service
class MathRpcService {

    @JsonRpcMethod("math.sum")
    public int sum(@JsonRpcParam("left") int left, @JsonRpcParam("right") int right) {
        return left + right;
    }

    @JsonRpcMethod
    public String ping() {
        return "pong";
    }
}
```

Name rule:

- `@JsonRpcMethod("math.sum")` -> explicit method name
- `@JsonRpcMethod` (empty value) -> Java method name (`ping`)

### 3.2 Manual style (`JsonRpcMethodRegistration` bean)

```java
import tools.jackson.databind.node.StringNode;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ManualRpcConfig {

    @Bean
    JsonRpcMethodRegistration manualPingRegistration() {
        return JsonRpcMethodRegistration.of("manual.ping", params -> StringNode.valueOf("pong-manual"));
    }
}
```

Use this when you need deterministic explicit registration without method scanning.

### 3.3 Typed style (`JsonRpcTypedMethodHandlerFactory`)

```java
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TypedRpcConfig {

    record UpperIn(String value) {}
    record UpperOut(String value) {}

    @Bean
    JsonRpcMethodRegistration typedUpperRegistration(JsonRpcTypedMethodHandlerFactory factory) {
        return JsonRpcMethodRegistration.of(
                "typed.upper",
                factory.unary(UpperIn.class, in -> new UpperOut(in.value().toUpperCase()))
        );
    }
}
```

Use this when you want compile-time DTO types and reuse the standard binder/writer pipeline.

## 4. Registration Priority and Conflict Policy

Two registration phases exist in auto-configuration:

1. `JsonRpcMethodRegistration` beans are applied while creating `JsonRpcDispatcher`.
2. `@JsonRpcMethod` scanner (`JsonRpcAnnotatedMethodRegistrar`) runs after singleton initialization and registers annotated handlers.

Within manual registrations, `orderedStream()` is used, so `@Order` / `Ordered` can control order.

Duplicate method names are governed by `jsonrpc.method-registration-conflict-policy`:

- `REJECT` (default): throws on duplicate registration.
- `REPLACE`: later registration replaces earlier registration.

Implications:

- With `REPLACE`, annotation phase can override previously registered manual handlers with the same name.
- With `REJECT`, duplication between manual and annotation styles fails fast.

Configuration:

```yaml
jsonrpc:
  method-registration-conflict-policy: REJECT
```

## 5. Parameter Mapping Semantics

### 5.1 Single parameter methods

`params` is mapped as a whole to the single declared parameter type.

```java
@JsonRpcMethod("greet")
public String greet(GreetParams params) {
    return "hello " + params.name();
}

record GreetParams(String name) {}
```

### 5.2 Multi parameter methods

Binding mode is selected by request `params` shape:

1. `params` is object -> named binding
2. otherwise -> positional array binding

Named binding name resolution order:

1. `@JsonRpcParam("...")`
2. Java reflection parameter name (`-parameters` required; already enabled in this project)

Example:

```java
@JsonRpcMethod("sum")
public int sum(@JsonRpcParam("left") int left, @JsonRpcParam("right") int right) {
    return left + right;
}
```

Request:

```json
{"jsonrpc":"2.0","method":"sum","params":{"left":1,"right":2},"id":1}
```

Positional example:

```java
@JsonRpcMethod("sum")
public int sum(int left, int right) {
    return left + right;
}
```

Request:

```json
{"jsonrpc":"2.0","method":"sum","params":[1,2],"id":1}
```

### 5.3 Return mapping

Return values are serialized through `JsonRpcResultWriter` (default uses Jackson `valueToTree`).

Supported practical types include:

- primitives/wrappers
- records/POJOs
- `Map`, `List`, collection types
- `JsonNode`

## 6. Control Scanning Scope

Disable annotation scanning when you only want explicit registrations:

```yaml
jsonrpc:
  scan-annotated-methods: false
```

## 7. Access Policy

Properties:

```yaml
jsonrpc:
  method-allowlist: [math.sum, ping]
  method-denylist: [admin.reset]
```

Rules:

1. Empty allowlist means all methods are allowed unless denied.
2. Non-empty allowlist means only listed methods are allowed.
3. Denylist always wins over allowlist.
4. `rpc.*` methods are blocked by registry regardless of allow/deny lists.

## 8. Notification Execution Strategy

Default is direct execution in the request thread.

Enable executor mode:

```yaml
jsonrpc:
  notification-executor-enabled: true
  notification-executor-bean-name: applicationTaskExecutor
```

Resolution order in executor mode:

1. Explicit `notification-executor-bean-name`
2. Single `Executor` bean in context
3. `applicationTaskExecutor`
4. Fallback to direct executor

If the configured bean name is missing, startup fails with an explicit error.

## 9. Metrics

If Micrometer `MeterRegistry` exists and `jsonrpc.metrics-enabled=true` (default), metrics interceptor/observer are enabled.

Key metrics:

- `jsonrpc.server.calls`
- `jsonrpc.server.latency`
- `jsonrpc.server.stage.events`
- `jsonrpc.server.failures`
- `jsonrpc.server.transport.errors`
- `jsonrpc.server.batch.*`
- `jsonrpc.server.notification.*`

Configuration:

```yaml
jsonrpc:
  metrics-enabled: true
  metrics-latency-histogram-enabled: true
  metrics-latency-percentiles: [0.9, 0.95, 0.99]
  metrics-max-method-tag-values: 100
```

## 10. Override Core Components

Auto-configuration uses `@ConditionalOnMissingBean`, so you can replace any component by defining your own bean.

Common override points:

- `JsonRpcRequestParser`
- `JsonRpcRequestValidator`
- `JsonRpcMethodRegistry`
- `JsonRpcMethodInvoker`
- `JsonRpcExceptionResolver`
- `JsonRpcResponseComposer`
- `JsonRpcNotificationExecutor`
- `JsonRpcHttpStatusStrategy`

Example HTTP status strategy:

```java
import com.limehee.jsonrpc.core.JsonRpcResponse;
import com.limehee.jsonrpc.spring.webmvc.JsonRpcHttpStatusStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;

@Configuration
class RpcHttpConfig {

    @Bean
    JsonRpcHttpStatusStrategy jsonRpcHttpStatusStrategy() {
        return new JsonRpcHttpStatusStrategy() {
            public HttpStatus statusForSingle(JsonRpcResponse response) { return HttpStatus.OK; }
            public HttpStatus statusForBatch(List<JsonRpcResponse> responses) { return HttpStatus.OK; }
            public HttpStatus statusForNotificationOnly() { return HttpStatus.NO_CONTENT; }
            public HttpStatus statusForParseError() { return HttpStatus.BAD_REQUEST; }
            public HttpStatus statusForRequestTooLarge() { return HttpStatus.PAYLOAD_TOO_LARGE; }
        };
    }
}
```

## 11. Next References

- Registration and binding deep dive: [`registration-and-binding.md`](registration-and-binding.md)
- Full property table and validation rules: [`configuration-reference.md`](configuration-reference.md)
- Extension design: [`extension-points.md`](extension-points.md)
