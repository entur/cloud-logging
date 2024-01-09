# GCP support
Stackdriver logging support (via console). Most features are opt-in via dependency import (this includes test dependencies).

## micrometer-gcp
Log severity metrics for Stackdriver.

# Spring web support
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
Logbook request-response-logging for local development.

## on-demand-spring-boot-starter-gcp-web
Selective (on-demand) logging for logging only interesting requests.

Try the [configuration](on-demand-spring-boot-starter-gcp-web/src/main/java/no/entur/logging/cloud/gcp/spring/web/properties/OndemandProperties.java)
```
entur.logging.http.ondemand.enabled=true
```

## Spring Web artifact coordinates
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
    <artifactId>request-response-spring-boot-starter-gcp-web</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-web-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
  <groupId>no.entur.logging.cloud</groupId>
  <artifactId>request-response-spring-boot-starter-gcp-web-test</artifactId>
  <version>${cloud-logging.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>no.entur.logging.cloud</groupId>
  <artifactId>on-demand-spring-boot-starter-gcp-web</artifactId>
  <version>${cloud-logging.version}</version>
  <scope>test</scope>
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
implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:spring-boot-starter-gcp-web-test:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web-test:${cloudLoggingVersion}")
implementation("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-web:${cloudLoggingVersion}")
```

</details>

# gRPC support

## spring-boot-starter-gcp-grpc
Machine-readable JSON log configuration for Stackdriver.

## spring-boot-starter-gcp-grpc-test
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

## request-response-spring-boot-starter-gcp-grpc
gRPC request-response-logging for Stackdriver.

## request-response-spring-boot-starter-gcp-grpc-test
gRPC request-response-logging for local development.

## on-demand-spring-boot-starter-gcp-grpc
Selective (on-demand) logging for logging only interesting requests.

Try the [configuration](on-demand-spring-boot-starter-gcp-grpc/src/main/java/no/entur/logging/cloud/gcp/spring/grpc/lognet/properties/OndemandProperties.java)
```
entur.logging.grpc.ondemand.enabled=true
```

## gRPC artifact coordinates
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
    <artifactId>request-response-spring-boot-starter-gcp-grpc</artifactId>
    <version>${cloud-logging.version}</version>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc-test</artifactId>
    <version>${cloud-logging.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
  <groupId>no.entur.logging.cloud</groupId>
  <artifactId>request-response-spring-boot-starter-gcp-grpc-test</artifactId>
  <version>${cloud-logging.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>no.entur.logging.cloud</groupId>
  <artifactId>on-demand-spring-boot-starter-gcp-grpc</artifactId>
  <version>${cloud-logging.version}</version>
  <scope>test</scope>
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
implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-test:${cloudLoggingVersion}")
testImplementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-test:${cloudLoggingVersion}")
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
