# GCP support
Stackdriver logging support (via console). 

## micrometer-gcp
Log severity metrics for Stackdriver.

# Spring web support
## spring-boot-starter-gcp-web
Machine-readable JSON log configuration for Stackdriver.

For on-demand logging, try the [configuration](spring-boot-starter-gcp-grpc-lognet/src/main/java/no/entur/logging/cloud/gcp/spring/grpc/lognet/properties/OndemandProperties.java)
```
entur.logging.grpc.ondemand.enabled=true
```

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
try (CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {

}
```

## request-response-spring-boot-starter-gcp-web
Logbook request-response-logging for Stackdriver.

## request-response-spring-boot-starter-gcp-web-test
Logbook request-response-logging for local development.

# gRPC support

## spring-boot-starter-gcp-grpc-lognet
Machine-readable JSON log configuration for Stackdriver.

## spring-boot-starter-gcp-grpc-lognet-test
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
try (CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {

}
```

## request-response-spring-boot-starter-gcp-grpc-lognet
gRPC request-response-logging for Stackdriver.

## request-response-spring-boot-starter-gcp-grpc-lognet-test
gRPC request-response-logging for local development.

# See also

## Testing
See [test-logback-junit](../test/test-logback-junit) for basic JUnit test support.
 
## Examples:

   * [gcp-grpc-example](../examples/gcp-grpc-example) Lognet gRPC example
   * [gcp-web-example](../examples/gcp-web-example) Spring-flavoured REST example
