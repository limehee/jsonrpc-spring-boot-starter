# Registration and Binding

This document explains every supported method registration style, how conflicts are resolved, and how parameters/results are bound.

## 1. Registration Styles

## 1.1 Annotation style: `@JsonRpcMethod`

Use on Spring-managed beans when `jsonrpc.scan-annotated-methods=true`.

```java
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcParam;
import org.springframework.stereotype.Service;

@Service
class UserRpcService {

    @JsonRpcMethod("user.find")
    public UserDto find(@JsonRpcParam("id") long id) {
        return new UserDto(id, "lime");
    }

    @JsonRpcMethod
    public String ping() {
        return "pong";
    }

    record UserDto(long id, String name) {}
}
```

Method name rule:

- explicit annotation value -> used as-is
- empty annotation value -> Java method name

## 1.2 Manual style: `JsonRpcMethodRegistration`

```java
import tools.jackson.databind.node.StringNode;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RpcRegistrationConfig {

    @Bean
    JsonRpcMethodRegistration pingRegistration() {
        return JsonRpcMethodRegistration.of("manual.ping", params -> StringNode.valueOf("pong-manual"));
    }
}
```

This style is explicit and works well for modular composition.

## 1.3 Typed adapter style: `JsonRpcTypedMethodHandlerFactory`

```java
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import com.limehee.jsonrpc.core.JsonRpcTypedMethodHandlerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TypedRegistrationConfig {

    record UpperIn(String value) {}
    record UpperOut(String value) {}

    @Bean
    JsonRpcMethodRegistration upperRegistration(JsonRpcTypedMethodHandlerFactory factory) {
        return JsonRpcMethodRegistration.of(
                "typed.upper",
                factory.unary(UpperIn.class, in -> new UpperOut(in.value().toUpperCase()))
        );
    }
}
```

Use this style when you want strict DTO-based mapping while still registering manually.

## 2. Registration Order and Priority

In Spring Boot auto-configuration, registration happens in two phases:

1. During `JsonRpcDispatcher` bean creation:
   - all `JsonRpcMethodRegistration` beans are registered
   - registration uses `ObjectProvider.orderedStream()`
   - `@Order` / `Ordered` affects order in this phase
2. After singletons are instantiated:
   - `JsonRpcAnnotatedMethodRegistrar` scans beans for `@JsonRpcMethod`
   - annotated methods are registered

Priority summary:

- manual registration phase runs first
- annotation phase runs second

## 3. Conflict Policy

Duplicate method names are controlled by `jsonrpc.method-registration-conflict-policy`.

### 3.1 `REJECT` (default)

- duplicate registration throws
- startup/runtime registration fails fast

### 3.2 `REPLACE`

- later registration replaces earlier one
- because annotation phase runs later, annotation can override manual registration for the same method name

Configuration:

```yaml
jsonrpc:
  method-registration-conflict-policy: REJECT
```

## 4. Parameter Binding Rules (`@JsonRpcMethod`)

## 4.1 Zero-parameter methods

Accepted request shapes include:

- `params` omitted
- `params: null`
- `params: {}`
- `params: []` (for typed no-arg adapters)

Non-empty parameters for no-arg methods produce `-32602`.

## 4.2 Single-parameter methods

- entire `params` node is converted to the parameter type
- conversion uses `JsonRpcParameterBinder` (default Jackson-based)

Example:

```java
@JsonRpcMethod("greet")
public String greet(GreetParams params) {
    return "hello " + params.name();
}

record GreetParams(String name) {}
```

## 4.3 Multi-parameter methods

Mode is chosen by request `params` shape:

1. Object -> named binding
2. Non-object -> positional binding (array expected)

### Named binding

Parameter name resolution order:

1. `@JsonRpcParam("name")`
2. reflection parameter name (requires `-parameters`)

Missing key for any argument -> `-32602`.

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

### Positional binding

Rules:

- `params` must be an array
- array size must exactly match method argument count

Example:

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

## 5. Supported Input and Output Shapes

Since binding/writing is Jackson-based, commonly supported types include:

- primitives and wrappers
- Java records
- POJOs/classes
- enums
- `List`, `Set`, `Map`, nested collections
- `JsonNode`

Examples:

- parameter: `List<String> tags`
- parameter: `Map<String, Integer> counters`
- return: `List<MyDto>`
- return: `Map<String, Object>`

## 6. Return Semantics

- Return values are serialized via `JsonRpcResultWriter`.
- Default writer uses `ObjectMapper.valueToTree`.
- Returning `null` is valid and results in `"result": null`.

## 7. Access Control vs Registration

Method registration and method access are separate concerns:

- registration phase stores handlers
- access policy interceptor checks allowlist/denylist at call time
- denied methods map to `-32601 Method not found` for non-disclosure behavior

## 8. Recommendations

1. Use annotation style for simple service classes.
2. Use manual/typed registration for reusable modules and deterministic wiring.
3. Keep duplicate names disabled (`REJECT`) unless you intentionally build layered overrides.
4. Prefer `@JsonRpcParam` for public APIs to avoid parameter-name compiler dependency issues.
