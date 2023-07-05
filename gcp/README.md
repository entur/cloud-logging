# gcp
Stackdriver logging support (via console). 

# micrometer-gcp
Log severity metrics for Stackdriver.

# spring-boot-starter-gcp
JSON log configuration for Stackdriver.

# spring-boot-starter-test gcp
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

# spring-boot-starter-logbook-gcp
Logbook request-response-logging for Stackdriver.

# spring-boot-starter-logbook-gcp-test
Logbook request-response-logging for local development.



