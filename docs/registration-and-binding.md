# Registration and Binding

This document explains all supported method registration styles and parameter binding rules.

## Registration Styles

### 1. Annotation Style: `@JsonRpcMethod`

Use on Spring-managed beans when annotation scanning is enabled.

```java
@Service
class UserRpcService {

    @JsonRpcMethod("user.find")
    public UserDto find(UserFindParams params) {
        return new UserDto(params.id(), "lime");
    }

    record UserFindParams(long id) {}
    record UserDto(long id, String name) {}
}
```

Method name resolution:

- `@JsonRpcMethod("custom.name")`: explicit name
- `@JsonRpcMethod` with empty value: Java method name is used

### 2. Manual Style: `JsonRpcMethodRegistration`

```java
@Bean
JsonRpcMethodRegistration ping() {
    return JsonRpcMethodRegistration.of("ping", params -> StringNode.valueOf("pong"));
}
```

Use this style for strict/manual registration control.

### 3. Typed Adapter Style: `JsonRpcTypedMethodHandlerFactory`

```java
@Bean
JsonRpcMethodRegistration upper(JsonRpcTypedMethodHandlerFactory factory) {
    return JsonRpcMethodRegistration.of(
        "upper",
        factory.unary(Input.class, in -> new Output(in.value().toUpperCase()))
    );
}
```

## Parameter Binding Rules (`@JsonRpcMethod`)

### Zero parameters

- Accepts `params` omitted, `null`, `{}`, or `[]` for typed no-arg adapters.
- Non-empty `params` produces `-32602`.

### Single parameter

- Entire `params` node is converted to parameter type via `JsonRpcParameterBinder`.
- Request-level validation allows `params` only as object/array (or omitted/null).
- Within that constraint, conversion follows Jackson mapping rules.

### Multiple parameters

Two modes are supported:

1. Positional array binding
2. Named object binding

### Positional array

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

Rules:

- `params` must be array
- array size must match parameter count exactly

### Named object binding

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

Rules:

- Parameter names are resolved in order:
  1. `@JsonRpcParam("name")`
  2. Java reflection parameter name (`-parameters` required)
- Missing named value triggers `-32602`.

## Supported Payload Types

Binding/writing is Jackson-based, so practical support includes:

- Primitive/wrapper types
- Java records
- POJOs
- `Map` / `List` / collection types (when used as declared method parameter/return type)
- `JsonNode`

## Return Value Rules

- Method return value is serialized via `JsonRpcResultWriter`.
- Default implementation uses `ObjectMapper.valueToTree`.
- Returning `null` is valid and serialized as JSON `null` in `result`.

## Conflict Policy

If the same method name is registered more than once, behavior is controlled by:

- `REJECT` (default): startup/runtime registration fails
- `REPLACE`: last registration replaces previous one

Property: `jsonrpc.method-registration-conflict-policy`
