# Azure support
azure Stackdriver logging support (via console). Most features are opt-in via dependency import (this includes test dependencies).

Features:

 * spring boot starters for web and gRPC
 * request-response logging
 * on-demand logging (i.e. extra logging give certain conditions)
 * test support for each of the above

The recommended approach is to include the corresponding test artifact for each feature.

## micrometer-azure
Log severity metrics for Azure.

# Spring web
## spring-boot-starter-azure-web
Machine-readable JSON log configuration for Stackdriver.

## spring-boot-starter-azure-web-test
Classic log configuration for local development.

Supported logging modes:

 * human-readable
   * plain logging (with colors)
   * JSON logging (with colors)
 * machine-readable
   * JSON

The logger assumes an ANSI-capable console / terminal during testing / building.

Toggle between modes using the `CompositeConsoleOutputControl` singleton:

```
try (Closeable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {

}
```

## request-response-spring-boot-starter-azure-web
Logbook request-response-logging for Stackdriver.

Note that remote JSON payloads are syntax validated and thus can be directly inlined in JSON log statements. 
Local JSON payloads are not syntax validated as the framework expect that we produce only valid JSON ourselves.
Use `RemoteHttpMessageContextSupplier` to skip syntax validation if remote JSON payloads are known to contain only valid JSON.

## request-response-spring-boot-starter-azure-web-test
Logbook request-response-logging for local development, for additional coloring and pretty-printing.

## on-demand-spring-boot-starter-azure-web
Selective (on-demand) logging for logging only interesting requests.

Enable using
```
entur.logging.http.ondemand.enabled=true
```

## Spring Web coordinates
Import the below artifacts:

<details>
  <summary>Maven coordinates</summary>

Add

```xml
<cloud-logging.version>4.0.x</cloud-logging>
```

and

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-azure-web</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-azure-web-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- request-response support -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-azure-web</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-azure-web-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- on-demand logging support -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-web</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- metrics -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>micrometer-azure</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<!-- additional log levels -->
<dependency>
  <groupId>no.entur.logging.cloud</groupId>
  <artifactId>api</artifactId>
  <version>${cloud-logging.version}</version>
</dependency>
```

</details>

or

<details>
  <summary>Gradle coordinates</summary>

For

```groovy
ext {
   cloudLoggingVersion = '4.0.x'
}
```

add

```groovy
implementation("no.entur.logging.cloud:spring-boot-starter-azure-web:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:spring-boot-starter-azure-web-test:${cloudLoggingVersion}")
// request response logging support
implementation("no.entur.logging.cloud:request-response-spring-boot-starter-azure-web:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:request-response-spring-boot-starter-azure-web-test:${cloudLoggingVersion}")
// on-demand logging support
implementation("no.entur.logging.cloud:on-demand-spring-boot-starter-web:${cloudLoggingVersion}")
// metrics
implementation("no.entur.logging.cloud:micrometer-azure:${cloudLoggingVersion}")
// logger with additional log levels
implementation("no.entur.logging.cloud:api:${cloudLoggingVersion}")


```

</details>

# gRPC

## spring-boot-starter-azure-grpc
Machine-readable JSON log configuration for Stackdriver.

## spring-boot-starter-azure-grpc-test
Classic (one line) log configuration for local development.

Supported logging modes:

* human-readable
   * plain logging (single line, with colors)
   * JSON logging (with colors)
* machine-readable
   * JSON

The logger assumes an ANSI-capable console / terminal during testing / building.

Toggle between modes using the `CompositeConsoleOutputControl` singleton:

```
try (Closeable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {

}
```

## request-response-spring-boot-starter-azure-grpc
gRPC request-response-logging for Stackdriver.

## request-response-spring-boot-starter-azure-grpc-test
gRPC request-response-logging for local development, for additional coloring and pretty-printing.

## on-demand-spring-boot-starter-azure-grpc
Selective (on-demand) logging for logging only interesting requests.

See the [configuration properties](../on-demand/on-demand-spring-boot-starter-gcp-grpc/src/main/java/no/entur/logging/cloud/spring/ondemand/grpc/properties/OndemandProperties.java)

```
entur.logging.grpc.ondemand.enabled=true
```

## gRPC coordinates
Import the below artifacts:

<details>
  <summary>Maven coordinates</summary>

Add

```xml
<cloud-logging.version>4.0.x</cloud-logging>
```

and

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-azure-grpc-ecosystem</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-azure-grpc-ecosystem-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- request-response logging -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-azure-grpc-ecosystem</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-azure-grpc-ecosystem-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- on-demand logging -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-grpc</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- metrics -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>micrometer-azure</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<!-- logger with additional log levels -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>api</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<!-- MDC support -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>mdc-context-grpc-netty</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<!-- Correlation id + various interceptors -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>correlation-id-trace-grpc-netty</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>correlation-id-trace-spring-boot-grpc</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
```

</details>

or

<details>
  <summary>Gradle coordinates</summary>

For

```groovy
ext {
   cloudLoggingVersion = '4.0.x'
}
```

add

```groovy
implementation("no.entur.logging.cloud:spring-boot-starter-azure-grpc-ecosystem:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:spring-boot-starter-azure-grpc-ecosystem-test:${cloudLoggingVersion}")
// requst-response logging
implementation("no.entur.logging.cloud:request-response-spring-boot-starter-azure-grpc-ecosystem:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:request-response-spring-boot-starter-azure-grpc-ecosystem-test:${cloudLoggingVersion}")
// on-demand logging support
implementation("no.entur.logging.cloud:on-demand-spring-boot-starter-grpc:${cloudLoggingVersion}")
// metrics
implementation("no.entur.logging.cloud:micrometer-azure:${cloudLoggingVersion}")
// logger with additional log levels
implementation("no.entur.logging.cloud:api:${cloudLoggingVersion}")
// MDC support
implementation project(':trace:mdc-context-grpc-netty')
// Correlation id + various interceptors
implementation project(':trace:server:correlation-id-trace-grpc-netty')
implementation project(':trace:server:correlation-id-trace-spring-boot-grpc')

```

</details>

# See also

## OWASP Dependency check supressions
Avoid [OWASP dependency supressions](dependencycheck-root-suppression.xml) flagging our dependencies as other people's dependencies.

<details>
  <summary>Suppress</summary>
  
```xml
 
    <suppress>
        <packageUrl regex="true">^pkg:maven/no\.entur\.logging\.cloud\/[a-z\.\-]*@.*$</packageUrl>
        <cpe>cpe:/a:grpc:grpc</cpe>
    </suppress>
    <suppress>
        <packageUrl regex="true">^pkg:maven/no\.entur\.abt\/[a-z\.\-]*@.*$</packageUrl>
        <cpe>cpe:/a:grpc:grpc</cpe>
    </suppress>
    <suppress>
        <cpe>cpe:/a:utils_project:utils</cpe>
    </suppress>
```

</details>

## Testing
See [test-logback-junit](../test/test-logback-junit) for basic JUnit test support.
 
## Examples:
See [examples](../examples) for various examples.
