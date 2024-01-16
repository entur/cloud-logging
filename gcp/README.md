# GCP support
GCP Stackdriver logging support (via console). Most features are opt-in via dependency import (this includes test dependencies).

Features:

 * spring boot starters for web and gRPC
 * request-response logging
 * on-demand logging (i.e. extra logging give certain conditions)
 * test support for each of the above

The recommended approach is to include the corresponding test artifact for each feature.

## micrometer-gcp
Log severity metrics for Stackdriver.

# Spring web
## spring-boot-starter-gcp-web
Machine-readable JSON log configuration for Stackdriver.

## spring-boot-starter-gcp-web-test
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
try (Closable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {

}
```

## request-response-spring-boot-starter-gcp-web
Logbook request-response-logging for Stackdriver.

Note that remote JSON payloads are syntax validated and thus can be directly inlined in JSON log statements. 
Local JSON payloads are not syntax validated as the framework expect that we produce only valid JSON ourselves.
Use `RemoteHttpMessageContextSupplier` to skip syntax validation if remote JSON payloads are known to contain only valid JSON.

## request-response-spring-boot-starter-gcp-web-test
Logbook request-response-logging for local development, for additional coloring and pretty-printing.

## on-demand-spring-boot-starter-gcp-web
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
<cloud-logging.version>2.0.x</cloud-logging>
```

and

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-web</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-web-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- request-respons support -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-web</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-web-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- on-demand logging support -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-gcp-web</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- metrics -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>micrometer-gcp</artifactId>
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
   cloudLoggingVersion = '2.0.x'
}
```

add

```groovy
implementation("no.entur.logging.cloud:spring-boot-starter-gcp-web:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:spring-boot-starter-gcp-web-test:${cloudLoggingVersion}")
// request response logging support
implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web-test:${cloudLoggingVersion}")
// on-demand logging support
implementation("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-web:${cloudLoggingVersion}")
// metrics
implementation("no.entur.logging.cloud:micrometer-gcp:${cloudLoggingVersion}")
// logger with additional log levels
implementation("no.entur.logging.cloud:api:${cloudLoggingVersion}")
```

</details>

# gRPC

## spring-boot-starter-gcp-grpc
Machine-readable JSON log configuration for Stackdriver.

## spring-boot-starter-gcp-grpc-test
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
try (Closable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {

}
```

## request-response-spring-boot-starter-gcp-grpc
gRPC request-response-logging for Stackdriver.

## request-response-spring-boot-starter-gcp-grpc-test
gRPC request-response-logging for local development, for additional coloring and pretty-printing.

## on-demand-spring-boot-starter-gcp-grpc
Selective (on-demand) logging for logging only interesting requests.

Try the [configuration](on-demand-spring-boot-starter-gcp-grpc/src/main/java/no/entur/logging/cloud/gcp/spring/grpc/lognet/properties/OndemandProperties.java)
```
entur.logging.grpc.ondemand.enabled=true
```

## gRPC coordinates
Import the below artifacts:

<details>
  <summary>Maven coordinates</summary>

Add

```xml
<cloud-logging.version>2.0.x</cloud-logging>
```

and

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- request-response logging -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- on-demand logging -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-gcp-grpc</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<!-- metrics -->
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>micrometer-gcp</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<!-- logger with additional log levels -->
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
   cloudLoggingVersion = '2.0.x'
}
```

add

```groovy
implementation("no.entur.logging.cloud:spring-boot-starter-gcp-grpc:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-test:${cloudLoggingVersion}")
// requst-response logging
implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-test:${cloudLoggingVersion}")
// on-demand logging support
implementation("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-grpc:${cloudLoggingVersion}")
// metrics
implementation("no.entur.logging.cloud:micrometer-gcp:${cloudLoggingVersion}")
// logger with additional log levels
implementation("no.entur.logging.cloud:api:${cloudLoggingVersion}")
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

   * [gcp-grpc-example](../examples/gcp-grpc-example) Lognet gRPC example
   * [gcp-web-example](../examples/gcp-web-example) Spring-flavoured REST example
   * [gcp-web-grpc-example](../examples/gcp-web-example) Spring-flavoured REST example with gRPC context (read: for further gRPC calls).
   * [gcp-grpc-without-test-artifacts-example](../examples/gcp-grpc-without-test-artifacts-example) Lognet gRPC example without test artifacts
   * [gcp-web-without-test-artifacts-example](../examples/gcp-web-without-test-artifacts-example) Spring-flavoured REST example without test artifacts
