# Getting started with servlet-based web
First remove any preexisting log configuration (i.e. logback.xml) and so on.

Then import the cloud-logging BOM:

<details>
  <summary>Maven BOM coordinates</summary>

Add

```xml
<cloud-logging.version>x.y.z</cloud-logging>
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
   cloudLoggingVersion = 'x.y.z'
}
```

add

```groovy
implementation platform("no.entur.logging.cloud:bom:${cloudLoggingVersion}")
testImplementation platform("no.entur.logging.cloud:bom:${cloudLoggingVersion}")
```
</details>

Please note that features below must be individually added __via artifact import__.

## Spring Boot starter
Add the spring-boot-starter artifact coordinates to your project.

<details>
  <summary>Maven Spring Boot Starter coordinates</summary>

```xml
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-web</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>spring-boot-starter-gcp-web-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:spring-boot-starter-gcp-web")
testImplementation ("no.entur.logging.cloud:spring-boot-starter-gcp-web-test")
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
    <artifactId>request-response-spring-boot-starter-gcp-web</artifactId>
</dependency>
<dependency>
    <groupId>no.entur.logging.cloud</groupId>
    <artifactId>request-response-spring-boot-starter-gcp-web-test</artifactId>
    <scope>test</scope>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web")
testImplementation ("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web-test")
```
</details>

Some default Logbook excludes are recommended: 

```
logbook:
  predicate:
    exclude:
      - path: /actuator/**
      - path: /favicon.*
      - path: /v2/api-docs/**
      - path: /v3/api-docs/**
      - path: /metrics
      - path: /swagger
```

Adjust the logger using

```
entur.logging.request-response.logger.level=INFO
entur.logging.request-response.logger.name=no.entur.logging.cloud
```

See [Logbook](https://github.com/zalando/logbook) for additional configuration options.

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
    <artifactId>on-demand-spring-boot-starter-gcp-web</artifactId>
</dependency>
```

</details>

or

<details>
  <summary>Gradle Spring Boot Starter coordinates</summary>

```groovy
implementation ("no.entur.logging.cloud:on-demand-spring-boot-starter-gcp-web")
```
</details>

While __disabled__ by default, on-demand logging can be enabled using

```
entur.logging.http.ondemand.enabled=true
```

Set the servlet order and pattern (defaults):

```
entur.logging.http.ondemand.filter-order=-2147483648
entur.logging.http.ondemand.filter-url-patterns=/*
```

Then configure log levels

```
entur.logging.http.ondemand.success.level=warn
entur.logging.http.ondemand.failure.level=info
```

where 

 * `success`: log level for the happy case 
   * cached log statements are discarded 
 * `failure`: log level for the unhappy case
   * cached log statements are printed

A `failure` can be triggered by

 * HTTP status codes
 * High severity log statements (i.e. warn or error) 

configured as by

``` 
entur.logging.http.ondemand.failure.http.status-code.equalOrHigherThan=400
# or
entur.logging.http.ondemand.failure.http.status-code.equal-to=404,405
# or
entur.logging.http.ondemand.failure.http.status-code.not-equal-to=200,201
```

and/or by log level severity

``` 
entur.logging.http.ondemand.failure.logger.level=error
```

optionally limited to specific loggers, i.e.

```
entur.logging.http.ondemand.failure.logger.name[0]=my.app
entur.logging.http.ondemand.failure.logger.name[1]=my.lib
```

There is also a `troubleshoot` variant

```
entur.logging.http.ondemand.troubleshoot.level=debug
entur.logging.http.ondemand.troubleshoot.http.headers[0].name=x-debug-this-request
```

which allows for additional logging in the precense of certain HTTP headers.

## Opting out
Some included features can be removed by excluding the corresponding artifacts:

 * micrometer
   * micrometer
   * micrometer-gcp
 * correlation id tracing
   * correlation-id-trace-spring-boot-web

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
      implementation("no.entur.logging.cloud:request-response-spring-boot-starter-gcp-web-test")
      implementation("no.entur.logging.cloud:spring-boot-starter-gcp-web-test")
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

## Recommended additions
Add `Prometheus` via [io.micrometer:micrometer-registry-prometheus](https://mvnrepository.com/artifact/io.micrometer/micrometer-registry-prometheus).

## Troubleshooting

### request-response logging not working
Did you import the relevant artifacts?

### on-demand logging not working
Did you import the relevant artifacts? 

Set property to enable.


