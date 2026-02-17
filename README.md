# jsonrpc-spring-boot-starter

Production-oriented JSON-RPC 2.0 server library for Java, with optional Spring WebMVC and Spring Boot integration.

## Why this library

- JSON-RPC 2.0 compliant core request/response/error handling
- Pure Java support (`jsonrpc-core`) for custom transports
- Spring WebMVC transport adapter and Spring Boot auto-configuration
- Multiple registration styles (annotation/manual/typed)
- Explicit extension points (parser/validator/invoker/exception mapping/interceptors/metrics)
- Focused dependency surface (no direct Guava, Commons Lang3, or Jakarta Validation dependency)

## Specification

- JSON-RPC 2.0: <https://www.jsonrpc.org/specification>
- RFC 8259 (JSON): <https://www.rfc-editor.org/rfc/rfc8259>

## Baseline

- Java: 17+
- Spring Boot baseline: 4.0.2
- Jackson baseline: 3.0.x
- Build: Gradle with Version Catalog
- CI matrix: Java 17, 21, 25

## Modules

| Module | Purpose |
|---|---|
| `jsonrpc-core` | Protocol model, parser/validator, dispatcher, method registry, typed binding |
| `jsonrpc-spring-webmvc` | HTTP endpoint adapter and HTTP status strategy |
| `jsonrpc-spring-boot-autoconfigure` | Property binding, bean wiring, method scanning, metrics/access integration |
| `jsonrpc-spring-boot-starter` | Starter dependency bundle for Spring Boot applications |

## Install

### Spring Boot starter

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

### Core only (pure Java)

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

## Quick Start (Spring Boot)

```java
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcParam;
import org.springframework.stereotype.Service;

@Service
class GreetingRpcService {

    @JsonRpcMethod("greet")
    public String greet(@JsonRpcParam("name") String name) {
        return "hello " + name;
    }
}
```

Default endpoint: `POST /jsonrpc`

Request:

```json
{"jsonrpc":"2.0","method":"greet","params":{"name":"developer"},"id":1}
```

Response:

```json
{"jsonrpc":"2.0","id":1,"result":"hello developer"}
```

## Quick Start (Pure Java)

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

JsonNode payload = mapper.readTree("""
{"jsonrpc":"2.0","method":"ping","id":1}
""");

JsonRpcDispatchResult result = dispatcher.dispatch(payload);
System.out.println(mapper.writeValueAsString(result.singleResponse().orElseThrow()));
```

## Registration Styles and Priority

Supported styles:

1. `@JsonRpcMethod` on Spring beans
2. `JsonRpcMethodRegistration` beans (manual)
3. `JsonRpcTypedMethodHandlerFactory` (typed adapter used by manual or custom registration)

Registration order in Spring Boot runtime:

1. `JsonRpcMethodRegistration` beans are registered first (`orderedStream()`; `@Order` applies).
2. `@JsonRpcMethod` scanning runs after singleton initialization and registers annotated methods.

Conflict behavior for duplicate method names is controlled by `jsonrpc.method-registration-conflict-policy`:

- `REJECT` (default): throws and fails startup/runtime registration.
- `REPLACE`: later registration replaces earlier one.

Practical implication with `REPLACE`: annotated methods can override manual registrations for the same name because annotation scanning executes later.

## Mapping and Binding Summary

- `@JsonRpcMethod("name")`: explicit JSON-RPC name.
- `@JsonRpcMethod` without value: Java method name is used.
- Multi-parameter binding mode:
  - `params` object -> named binding (`@JsonRpcParam` first, then Java parameter names with `-parameters`)
  - `params` array -> positional binding (exact argument count required)
- Single parameter -> entire `params` node mapped to declared type.
- Return values are serialized via Jackson (`ObjectMapper.valueToTree` by default).

## Build and Verify

```bash
./gradlew check
./gradlew apiCompat -PapiBaselineVersion=<released-version>
./gradlew :jsonrpc-core:jmh
./gradlew :jsonrpc-core:jmhQuick
./scripts/verify-consumer-smoke.sh
```

## Documentation

Detailed docs are under [`docs/`](docs/):

- [`docs/index.md`](docs/index.md)
- [`docs/getting-started.md`](docs/getting-started.md)
- [`docs/spring-boot-guide.md`](docs/spring-boot-guide.md)
- [`docs/pure-java-guide.md`](docs/pure-java-guide.md)
- [`docs/registration-and-binding.md`](docs/registration-and-binding.md)
- [`docs/configuration-reference.md`](docs/configuration-reference.md)
- [`docs/extension-points.md`](docs/extension-points.md)
- [`docs/protocol-and-compliance.md`](docs/protocol-and-compliance.md)
- [`docs/testing-and-quality.md`](docs/testing-and-quality.md)
- [`docs/performance.md`](docs/performance.md)
- [`docs/troubleshooting.md`](docs/troubleshooting.md)

## Sample

- Spring Boot sample app: [`samples/spring-boot-demo`](samples/spring-boot-demo)

Run:

```bash
./gradlew -p samples/spring-boot-demo bootRun
```

## Project Docs

- Contributing: [`CONTRIBUTING.md`](CONTRIBUTING.md)
- Security: [`SECURITY.md`](SECURITY.md)
- Release checklist: [`docs/release-checklist.md`](docs/release-checklist.md)
- Changelog: [`CHANGELOG.md`](CHANGELOG.md)
