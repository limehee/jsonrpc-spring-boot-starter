#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="${JSONRPC_VERSION:-$(grep '^version=' "${ROOT_DIR}/gradle.properties" | cut -d'=' -f2)}"
SPRING_BOOT_VERSION="${SPRING_BOOT_VERSION:-4.0.2}"

if ! command -v mvn >/dev/null 2>&1; then
  echo "mvn is required but was not found in PATH"
  exit 1
fi

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

echo "[consumer-smoke] publish artifacts to mavenLocal (${VERSION})"
"${ROOT_DIR}/gradlew" --no-daemon --configuration-cache publishToMavenLocal

echo "[consumer-smoke] create Maven consumer project"
MAVEN_DIR="${TMP_DIR}/consumer-maven-core"
mkdir -p "${MAVEN_DIR}/src/test/java/com/example"
cat > "${MAVEN_DIR}/pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>consumer-maven-core</artifactId>
  <version>1.0.0</version>
  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>5.13.4</junit.version>
  </properties>
  <dependencies>
    <dependency>
      <groupId>io.github.limehee</groupId>
      <artifactId>jsonrpc-core</artifactId>
      <version>${VERSION}</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>\${junit.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.5.4</version>
      </plugin>
    </plugins>
  </build>
</project>
EOF

cat > "${MAVEN_DIR}/src/test/java/com/example/CoreConsumerSmokeTest.java" <<'EOF'
package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreConsumerSmokeTest {

    @Test
    void dispatchesPingFromPublishedArtifact() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonRpcDispatcher dispatcher = new JsonRpcDispatcher();
        dispatcher.register("ping", params -> TextNode.valueOf("pong"));

        JsonNode payload = mapper.readTree("{\"jsonrpc\":\"2.0\",\"method\":\"ping\",\"id\":1}");
        JsonRpcDispatchResult result = dispatcher.dispatch(payload);

        assertEquals("pong", result.singleResponse().orElseThrow().result().asText());
    }
}
EOF

echo "[consumer-smoke] run Maven consumer test"
mvn -f "${MAVEN_DIR}/pom.xml" -q test

echo "[consumer-smoke] create Gradle consumer project"
GRADLE_DIR="${TMP_DIR}/consumer-gradle-starter"
mkdir -p "${GRADLE_DIR}/src/main/java/com/example" "${GRADLE_DIR}/src/test/java/com/example"
cat > "${GRADLE_DIR}/settings.gradle" <<'EOF'
rootProject.name = 'consumer-gradle-starter'
EOF

cat > "${GRADLE_DIR}/build.gradle" <<EOF
plugins {
    id 'java'
    id 'org.springframework.boot' version '${SPRING_BOOT_VERSION}'
}

group = 'com.example'
version = '1.0.0'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.github.limehee:jsonrpc-spring-boot-starter:${VERSION}'
    testImplementation 'org.springframework.boot:spring-boot-starter-test:${SPRING_BOOT_VERSION}'
}

tasks.withType(Test).configureEach {
    useJUnitPlatform()
}
EOF

cat > "${GRADLE_DIR}/src/main/java/com/example/ConsumerApplication.java" <<'EOF'
package com.example;

import com.limehee.jsonrpc.core.JsonRpcMethod;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class ConsumerApplication {
}

@Service
class GreetingRpcService {
    @JsonRpcMethod("greet")
    public String greet(GreetParams params) {
        return "hello " + params.name();
    }

    record GreetParams(String name) {
    }
}
EOF

cat > "${GRADLE_DIR}/src/test/java/com/example/StarterConsumerSmokeTest.java" <<'EOF'
package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.limehee.jsonrpc.core.JsonRpcDispatchResult;
import com.limehee.jsonrpc.core.JsonRpcDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class StarterConsumerSmokeTest {

    @Autowired
    private JsonRpcDispatcher dispatcher;

    @Test
    void invokesRegisteredMethodFromPublishedStarter() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonRpcDispatchResult result = dispatcher.dispatch(mapper.readTree(
                "{\"jsonrpc\":\"2.0\",\"method\":\"greet\",\"params\":{\"name\":\"developer\"},\"id\":1}"));
        assertEquals("hello developer", result.singleResponse().orElseThrow().result().asText());
    }
}
EOF

echo "[consumer-smoke] run Gradle consumer test"
"${ROOT_DIR}/gradlew" --no-daemon -p "${GRADLE_DIR}" test

echo "[consumer-smoke] success"
