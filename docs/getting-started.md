# Getting Started

This guide gets a JSON-RPC 2.0 endpoint running quickly and points to deeper references.

## 1. Prerequisites

- JDK 17 or newer
- For Spring Boot usage: Spring Boot application (baseline tested with 4.0.2)
- JSON payloads sent as `application/json`

## 2. Choose Dependency

### Spring Boot Starter

Maven:

```xml
<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.limehee:jsonrpc-spring-boot-starter:0.1.0-SNAPSHOT")
```

Gradle (Groovy DSL):

```groovy
implementation 'io.github.limehee:jsonrpc-spring-boot-starter:0.1.0-SNAPSHOT'
```

### Core Only (Plain Java)

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

Gradle (Groovy DSL):

```groovy
implementation 'io.github.limehee:jsonrpc-core:0.1.0-SNAPSHOT'
```

## 3. Spring Boot Minimum Example

```java
import com.limehee.jsonrpc.core.JsonRpcMethod;
import org.springframework.stereotype.Service;

@Service
class GreetingRpcService {
    @JsonRpcMethod("greet")
    public String greet(GreetParams params) {
        return "hello " + params.name();
    }

    record GreetParams(String name) {}
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

## 4. Core-Only Minimum Example (No Spring)

```java
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;

ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();

dispatcher.register("ping", params -> StringNode.valueOf("pong"));

JsonNode payload = mapper.readTree("""
{"jsonrpc":"2.0","method":"ping","id":1}
""");

JsonRpcDispatchResult result = dispatcher.dispatch(payload);
System.out.println(result.singleResponse().orElseThrow());
```

## 5. Verify with cURL

```bash
curl -sS -X POST http://localhost:8080/jsonrpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"greet","params":{"name":"rpc"},"id":1}'
```

## 6. Next Steps

- Spring customization: [`spring-boot-guide.md`](spring-boot-guide.md)
- Protocol details and RFC alignment: [`protocol-and-compliance.md`](protocol-and-compliance.md)
- Property and validation rules: [`configuration-reference.md`](configuration-reference.md)
