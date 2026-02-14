# jsonrpc-spring-boot-starter

JSON-RPC 2.0 server components for Spring Boot.
Baseline: Spring Boot 4.0.2, Gradle 9.3.1.

## Modules

- `jsonrpc-core`: JSON-RPC 2.0 protocol model and dispatch pipeline.
- `jsonrpc-spring-webmvc`: Spring WebMVC transport adapter.
- `jsonrpc-spring-boot-autoconfigure`: Spring Boot auto-configuration.
- `jsonrpc-spring-boot-starter`: starter dependency bundle.

## Quick Start

Add starter dependency:

```xml
<dependency>
  <groupId>io.github.limehee</groupId>
  <artifactId>jsonrpc-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Register JSON-RPC methods as Spring beans:

```java
@Bean
JsonRpcMethodRegistration pingMethod() {
    return JsonRpcMethodRegistration.of("ping", params -> TextNode.valueOf("pong"));
}
```

Typed registration is also available:

```java
@Bean
JsonRpcMethodRegistration greet(JsonRpcTypedMethodHandlerFactory factory) {
    return JsonRpcMethodRegistration.of(
            "greet",
            factory.unary(GreetParams.class, params -> "hello " + params.name()));
}
```

Or annotate bean methods for auto-registration:

```java
class GreetingService {
    @JsonRpcMethod("greet")
    public String greet(GreetParams params) {
        return "hello " + params.name();
    }
}
```

Default endpoint is `POST /jsonrpc`.

## Configuration

- `jsonrpc.path` (default `/jsonrpc`)
- `jsonrpc.max-batch-size` (default `100`)
- `jsonrpc.max-request-bytes` (default `1048576`)
- `jsonrpc.method-namespace-policy` (default `DISALLOW_RPC_PREFIX`)
- `jsonrpc.scan-annotated-methods` (default `true`)
- `jsonrpc.include-error-data` (default `false`)
- `jsonrpc.metrics-enabled` (default `true`, requires `MeterRegistry`)
- `jsonrpc.method-allowlist` (default empty)
- `jsonrpc.method-denylist` (default empty)

## Build

```bash
./gradlew test
```

## Publish

Set OSSRH and signing credentials via `gradle.properties` or environment variables:

- `OSSRH_USERNAME`, `OSSRH_PASSWORD`
- `SIGNING_KEY`, `SIGNING_PASSWORD`

Then run:

```bash
./gradlew publish
```
