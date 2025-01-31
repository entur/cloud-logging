# Getting started with gRPC
First remove any preexisting log configuration (i.e. logback.xml) and so on.

Then import the cloud-logging BOM:

<details>
  <summary>Maven BOM coordinates</summary>

Add

```xml
<cloud-logging.version>4.0.x</cloud-logging>
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
    <artifactId>spring-boot-starter-gcp-grpc-ecosystem</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-grpc-ecosystem-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-ecosystem")
testImplementation ("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-ecosystem-test")
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
    <artifactId>request-response-spring-boot-starter-gcp-grpc-ecosystem</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-grpc-ecosystem-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-ecosystem")
testImplementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-ecosystem-test")
```
</details>

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

### On-demand logging
This feature adjusts the log level for individual web server requests, taking into account actual behaviour.

* increase log level for happy-cases (i.e. WARN or ERROR), otherwise
* reduce log level (i.e. INFO) for
    * unexpected HTTP response codes
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

Add the `GrpcLoggingScopeContextInterceptor` interceptor to your gRPC services.

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
      implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-grpc-ecosystem-test")
      implementation("no.entur.logging.cloud:spring-boot-starter-gcp-grpc-ecosystem-test")
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

## Running applications locally
For 'classic' one-line log output when running a server locally, additionally add the logging test artifacts to the main scope during local execution only.

* Maven: Use profiles
* Gradle:
    * Use configurations, and/or
    * add dependencies directly to task

<details>
  <summary>Gradle bootRun example</summary>

```groovy
bootRun {
    dependencies {
        implementation("no.entur.logging.cloud:spring-boot-starter-gcp-web-test")
        implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web-test")
    }
}
```

</details>

## Toggle output mode using profiles
Add an event listener to set your preferred log output:

```
@Component
@Profile("local")
public class HumanReadableJsonApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        CompositeConsoleOutputControl.useHumanReadableJsonEncoder();
    }
}
```

## Troubleshooting

### request-response logging not working
Did you import the relevant artifacts?

### on-demand logging not working
Did you import the relevant artifacts?

Set property to enable.

