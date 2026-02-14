# Spring Boot Guide

This guide covers practical usage in Spring Boot applications.

## 1. Dependency

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

## 2. Endpoint Exposure

Auto-configured endpoint:

- Method: `POST`
- Path: `jsonrpc.path` (default `/jsonrpc`)
- Content type: `application/json`

Minimal app properties:

```yaml
jsonrpc:
  path: /jsonrpc
```

## 3. Method Registration Approaches

### A. Annotation-based (`@JsonRpcMethod`)

```java
import com.limehee.jsonrpc.core.JsonRpcMethod;
import com.limehee.jsonrpc.core.JsonRpcParam;
import org.springframework.stereotype.Service;

@Service
class GreetingRpcService {

    @JsonRpcMethod("ping")
    public String ping() {
        return "pong";
    }

    @JsonRpcMethod("sum")
    public int sum(@JsonRpcParam("left") int left, @JsonRpcParam("right") int right) {
        return left + right;
    }
}
```

### B. Explicit registration bean (`JsonRpcMethodRegistration`)

```java
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcMethodRegistration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RpcRegistrationConfig {

    @Bean
    JsonRpcMethodRegistration pingRegistration() {
        return JsonRpcMethodRegistration.of("manual.ping", params -> TextNode.valueOf("pong-manual"));
    }
}
```

### C. Typed factory (`JsonRpcTypedMethodHandlerFactory`)

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
    JsonRpcMethodRegistration upperRegistration(JsonRpcTypedMethodHandlerFactory factory) {
        return JsonRpcMethodRegistration.of(
                "typed.upper",
                factory.unary(UpperIn.class, in -> new UpperOut(in.value().toUpperCase()))
        );
    }
}
```

## 4. Customization via Bean Override

Auto-configuration uses `@ConditionalOnMissingBean` on core components. You can override:

- `JsonRpcRequestParser`
- `JsonRpcRequestValidator`
- `JsonRpcMethodRegistry`
- `JsonRpcMethodInvoker`
- `JsonRpcExceptionResolver`
- `JsonRpcResponseComposer`
- `JsonRpcNotificationExecutor`
- `JsonRpcHttpStatusStrategy`

Example: custom HTTP status strategy.

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

## 5. Annotation Scan Control

`jsonrpc.scan-annotated-methods=true` (default) enables scanning for `@JsonRpcMethod` on beans.

Disable when you want only explicit registrations:

```yaml
jsonrpc:
  scan-annotated-methods: false
```

## 6. Access Policy and Metrics

Access policy:

```yaml
jsonrpc:
  method-allowlist: [ping, greet]
  method-denylist: [admin.reset]
```

- If a method exists in both allowlist and denylist, denylist wins.
- Reserved `rpc.*` method names are always blocked at registration time.

Metrics (Micrometer) are enabled by default when `MeterRegistry` is present:

- `jsonrpc.server.calls`
- `jsonrpc.server.latency`
- `jsonrpc.server.stage.events`
- `jsonrpc.server.failures`
- `jsonrpc.server.transport.errors`
- `jsonrpc.server.batch.requests`
- `jsonrpc.server.batch.entries`
- `jsonrpc.server.batch.size`
- `jsonrpc.server.notification.queue.delay`
- `jsonrpc.server.notification.execution`
- `jsonrpc.server.notification.submitted`
- `jsonrpc.server.notification.failed`

Tune metrics behavior:

```yaml
jsonrpc:
  metrics-enabled: true
  metrics-latency-histogram-enabled: true
  metrics-latency-percentiles: [0.9, 0.95, 0.99]
  metrics-max-method-tag-values: 100
```

Disable metrics:

```yaml
jsonrpc:
  metrics-enabled: false
```

## 7. Notification Execution Strategy

Default notification execution is direct (same thread).

Enable executor-based dispatch:

```yaml
jsonrpc:
  notification-executor-enabled: true
  notification-executor-bean-name: applicationTaskExecutor
```

Resolution order when enabled:

1. Named executor from `notification-executor-bean-name`
2. Only one `Executor` bean available
3. `applicationTaskExecutor` bean
4. Fallback to direct execution

## 8. Where Next

- Property table and validation: [`configuration-reference.md`](configuration-reference.md)
- Parameter binding rules: [`registration-and-binding.md`](registration-and-binding.md)
- Extension points: [`extension-points.md`](extension-points.md)
