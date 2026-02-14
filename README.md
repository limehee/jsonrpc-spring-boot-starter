# jsonrpc-spring-boot-starter

JSON-RPC 2.0 server components for Spring Boot.
Baseline: Spring Boot 4.0.2, Gradle 9.3.1.

## Modules

- `jsonrpc-core`: JSON-RPC 2.0 domain model and dispatcher.
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

Default endpoint is `POST /jsonrpc`.

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
