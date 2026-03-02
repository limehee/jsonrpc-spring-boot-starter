# Getting Started

This guide gets a working JSON-RPC endpoint up quickly and provides the shortest path to deeper docs.

## 1. Prerequisites

- JDK 17+
- JSON request body (`application/json`)
- Optional for Spring Boot path: Boot 4.x application

## 2. Choose a Runtime Style

- Spring Boot + WebMVC endpoint: use `jsonrpc-spring-boot-starter`
- Pure Java/custom transport: use `jsonrpc-core`

## 3. Dependency Setup

### 3.1 Spring Boot starter

Replace `latest-version` with the release you want to use.

Maven:

```xml
<properties>
  <jsonrpc.version>latest-version</jsonrpc.version>
</properties>

<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-spring-boot-starter</artifactId>
  <version>${jsonrpc.version}</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
val jsonrpcVersion = "latest-version"

dependencies {
    implementation("io.github.limehee:jsonrpc-spring-boot-starter:$jsonrpcVersion")
}
```

Gradle (Groovy DSL):

```groovy
def jsonrpcVersion = "latest-version"

dependencies {
    implementation "io.github.limehee:jsonrpc-spring-boot-starter:${jsonrpcVersion}"
}
```

Gradle Version Catalog (`libs.versions.toml`):

```toml
[versions]
jsonrpc = "latest-version"

[libraries]
jsonrpc-spring-boot-starter = { module = "io.github.limehee:jsonrpc-spring-boot-starter", version.ref = "jsonrpc" }
```

```kotlin
dependencies {
    implementation(libs.jsonrpc.spring.boot.starter)
}
```

### 3.2 Core only (pure Java)

Replace `latest-version` with the release you want to use.

Maven:

```xml
<properties>
  <jsonrpc.version>latest-version</jsonrpc.version>
</properties>

<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-core</artifactId>
  <version>${jsonrpc.version}</version>
</dependency>
```

Gradle (Kotlin DSL):

```kotlin
val jsonrpcVersion = "latest-version"

dependencies {
    implementation("io.github.limehee:jsonrpc-core:$jsonrpcVersion")
}
```

Gradle (Groovy DSL):

```groovy
def jsonrpcVersion = "latest-version"

dependencies {
    implementation "io.github.limehee:jsonrpc-core:${jsonrpcVersion}"
}
```

Gradle Version Catalog (`libs.versions.toml`):

```toml
[versions]
jsonrpc = "latest-version"

[libraries]
jsonrpc-core = { module = "io.github.limehee:jsonrpc-core", version.ref = "jsonrpc" }
```

```kotlin
dependencies {
    implementation(libs.jsonrpc.core)
}
```

## 4. Spring Boot Minimal Example

Service registration with annotation:

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

Default endpoint:

- HTTP method: `POST`
- path: `/jsonrpc`

Request:

```json
{"jsonrpc":"2.0","method":"greet","params":{"name":"developer"},"id":1}
```

Response:

```json
{"jsonrpc":"2.0","id":1,"result":"hello developer"}
```

## 5. Pure Java Minimal Example

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
System.out.println(mapper.writeValueAsString(result.singleResponse().orElseThrow()));
```

## 6. Verify with cURL

```bash
curl -sS -X POST http://localhost:8080/jsonrpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"greet","params":{"name":"rpc"},"id":1}'
```

## 7. What to Read Next

- Full Spring setup, registration styles, and operational options: [`spring-boot-guide.md`](spring-boot-guide.md)
- Pure Java advanced composition and custom transport patterns: [`pure-java-guide.md`](pure-java-guide.md)
- Registration priority and parameter binding rules: [`registration-and-binding.md`](registration-and-binding.md)
- Property semantics, validation, and precedence: [`configuration-reference.md`](configuration-reference.md)
