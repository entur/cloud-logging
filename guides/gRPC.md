# Getting started with Lognet gRPC
First remove any preexisting log configuration (i.e. logback.xml) and so on.

Then import the cloud-logging BOM:

<details>
  <summary>Maven BOM coordinates</summary>

Add

```xml
<cloud-logging.version>2.0.x</cloud-logging>
```

and

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>bom</artifactId>
    <version>${cloud-logging.version}</version>
    <type>pom</type>
    <scope>import</scope>    
</dependency>
```

</details>

or

<details>
  <summary>Gradle BOM coordinates</summary>

For

```groovy
ext {
   cloudLoggingVersion = '2.0.x'
}
```

add

```groovy
implementation platform("no.entur.logging.cloud:bom:${cloudLoggingVersion}")
testImplementation platform("no.entur.logging.cloud:bom:${cloudLoggingVersion}")
```
</details>

## Spring Boot starter
Add the spring-boot-starter artifact coordinates to your project.

<details>
  <summary>Maven Spring Boot Starter coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:spring-boot-starter-gcp-grpc")
testImplementation ("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-test")
```
</details>

Verify configuration by toggling between output modes in a unit test:

```
try (Closeable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
    // your logging here
}
```

For additional error levels, try the [DevOpsLogger](../api):

```
DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(MyClass.class);

// ... your code here

LOGGER.errorTellMeTomorrow("Error statement");
LOGGER.errorInterruptMyDinner("Critical statement");
LOGGER.errorWakeMeUpRightNow("Alert statement");
```

Configure log levels via Spring, i.e. `application.properties` like

```
logging.level.root=INFO
logging.level.my.package=WARN
```

### Request-response logging
Import the request-response Spring Boot starters:

<details>
  <summary>Maven Spring Boot Starter coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc")
testImplementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-test")
```
</details>

### On-demand logging
Import the on-demand Spring Boot starters:

<details>
  <summary>Maven Spring Boot Starter coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-gcp-grpc</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-gcp-grpc-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-grpc")
testImplementation ("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-grpc-test")
```
</details>
