# Getting started with gRPC
First remove any preexisting log configuration (i.e. logback.xml) and so on.

Then import the cloud-logging BOM:

<details>
  <summary>Maven BOM coordinates</summary>

Add

```xml
<properties>
    <cloud-logging.version>x.y.z</cloud-logging.version>
</properties>
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
   cloudLoggingVersion = '4.0.x'
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
    <artifactId>spring-boot-starter-gcp-grpc-spring</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc-spring-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-spring")
testImplementation ("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-spring-test")
```
</details>

Verify configuration by toggling between output modes in a unit test using the `CompositeConsoleOutputControl` class.

### Toggle log output-format during testing

```
try (Closeable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
    // your logging here
}
```

Configure log levels via Spring, i.e. `application.properties` like

```
logging.level.root=INFO
logging.level.my.package=WARN
```

### Additional log levels
For additional error levels, try the [DevOpsLogger](../api):

```
DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(MyClass.class);

// ... your code here

LOGGER.errorTellMeTomorrow("Error statement");
LOGGER.errorInterruptMyDinner("Critical statement");
LOGGER.errorWakeMeUpRightNow("Alert statement");
```

<details>
  <summary>Maven Logger API coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>api</artifactId>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Logger API coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:api")
```
</details>

### Optional: Request-response logging
Import the request-response Spring Boot starters:

<details>
  <summary>Maven Spring Boot Starter coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc-spring</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc-spring-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-spring")
testImplementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-spring-test")
```
</details>

Please note:

 * GCP `LogEntry` has a hard size limit of 256 KiB. Request/response bodies are truncated at 200 KiB by default to stay safely within this limit, accounting for headers, MDC fields, and other log-framework overhead. Override the default using:
    ```
    entur.logging.request-response.max-body-size=<bytes>
    entur.logging.request-response.max-size=<bytes>
    ```
    See also: [Too long lines](troubleShooting.md#too-long-lines).

Adjust the logger using

```
entur.logging.request-response.logger.level=INFO
entur.logging.request-response.logger.name=no.entur.logging.cloud
```

Set `OrderedGrpcLoggingServerInterceptor` order using

```
entur.logging.request-response.grpc.server.interceptor-order=0
```

also add `RequestResponseGRpcExceptionHandlerInterceptor` error handler before the response logging, so that the response status is logged correctly

```
entur.logging.request-response.grpc.server.exception-handler.interceptor-order=0
```

Optionally also configure the `OrderedGrpcLoggingClientInterceptor` order using

```
entur.logging.request-response.grpc.client.interceptor-order=0
```

Also create your own beans for 

 * `JsonFormat.TypeRegistry`
 * `GrpcStatusMapper`
* `GrpcPayloadJsonMapper`
* `GrpcMetadataJsonMapper`
* `GrpcClientLoggingFilters`

to further customize logging.

### Optional: On-demand logging
This feature adjusts the log level for individual gRPC service requests, taking into account actual behaviour.

* increase log level for happy-cases (i.e. WARN or ERROR), otherwise
* reduce log level (i.e. INFO) for
    * unexpected gRPC status codes
    * unexpected log statement levels (i.e. ERROR)
    * unexpectedly long call duration
    * troubleshooting

Import the on-demand Spring Boot starters:

<details>
  <summary>Maven Spring Boot Starter coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>on-demand-spring-boot-starter-gcp-grpc</artifactId>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-grpc")
```
</details>

While __disabled__ by default, on-demand logging can be enabled using

```
entur.logging.grpc.ondemand.enabled=true
```

Add the `GrpcLoggingScopeContextInterceptor` interceptor to your gRPC services (see [examples](../examples) for guidance on interceptor ordering).

With on-demand logging enabled, request-response logging only converts message bodies to JSON for log statements which are actually written, so discarded statements cost very little. The interceptors keep a reference to the protobuf message and convert it later, possibly on another thread (the one that flushes the on-demand scope, before the log statement is handed over to the async appender). This is safe because generated protobuf messages are immutable (see [Java Generated Code Guide: Messages](https://protobuf.dev/reference/java/java-generated/#message) and [Protocol Buffer Basics: Java](https://protobuf.dev/getting-started/javatutorial/)), and gRPC's protobuf marshaller does not let received messages refer to its reused read buffer (see [ProtoLiteUtils](https://github.com/grpc/grpc-java/blob/master/protobuf-lite/src/main/java/io/grpc/protobuf/lite/ProtoLiteUtils.java)). Message builders are not thread-safe, so they are converted right away. Messages which are too large to be logged (binary size larger than half of `entur.logging.request-response.max-body-size`) are also converted right away, as that only produces a short description, so that they are not kept in memory until the call completes.

By default, a body is converted just before the log statement is handed over to the async appender, i.e. on the thread which flushes the on-demand scope, so the async appender's (single) worker thread is not burdened with protobuf-to-JSON conversion. To instead leave the conversion to the async appender's thread, which takes the work off the thread which flushes the scope (i.e. the thread which handles the failing call), use

```
entur.logging.request-response.grpc.map-body-on-async-appender-thread=true
```

Whichever thread converts the body, it is converted at most once. If a log statement never passes through the on-demand async appender, the body is converted when the log statement is written. The property has no effect unless on-demand logging is enabled; bodies are then converted right away.

Then configure log levels

```
entur.logging.grpc.ondemand.success.level=warn
entur.logging.grpc.ondemand.failure.level=info
```

where

 * `success`: log level for the happy case
   * cached log statements are discarded
 * `failure`: log level for the unhappy case
   * cached log statements are printed

A `failure` can be triggered by high severity log statements (i.e. warn or error), configured as

```
entur.logging.grpc.ondemand.failure.logger.level=error
```

optionally limited to specific loggers, i.e.

```
entur.logging.grpc.ondemand.failure.logger.name[0]=my.app
entur.logging.grpc.ondemand.failure.logger.name[1]=my.lib
```

There is also a `troubleshoot` variant

```
entur.logging.grpc.ondemand.troubleshoot.level=debug
entur.logging.grpc.ondemand.troubleshoot.grpc.headers[0].name=x-debug-this-request
```

which allows for additional logging in the presence of certain gRPC headers.

## Running applications locally
For 'classic' one-line log output when running a server locally, additionally add the logging test artifacts to the main scope during local execution only.

* Maven: Use profiles
* Gradle:
    * Use configurations, and/or
    * add dependencies directly to task

<details>
  <summary>Gradle bootRun example</summary>

```groovy
tasks.register("logPlainly") {
   dependencies {
      implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-spring-test")
      implementation("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-spring-test")
   }
}

tasks.withType(JavaExec).configureEach {
   dependsOn("logPlainly")
}
```

Then configure desired output by specifying `entur.logging.style`

```
entur.logging.style=humanReadablePlain|humanReadableJson|machineReadableJson
```
</details>

## Opting out
Some included features can be removed by excluding the corresponding artifacts:

* micrometer
  * micrometer
  * micrometer-gcp
* correlation id trace
  * correlation-id-trace-spring-boot-grpc

## Recommended additions
Add `Prometheus` via [io.micrometer:micrometer-registry-prometheus](https://mvnrepository.com/artifact/io.micrometer/micrometer-registry-prometheus).

## Troubleshooting

### request-response logging not working
Did you import the relevant artifacts? Both the main and test artifacts must be added.

### on-demand logging not working
Did you import the relevant artifact?

The feature is disabled by default; set the following property to enable it:

```
entur.logging.grpc.ondemand.enabled=true
```

Also verify that the `GrpcLoggingScopeContextInterceptor` interceptor is registered for your gRPC service.

