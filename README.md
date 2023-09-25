# cloud-logging
Entur cloud logging libraries. Mostly based on

 * SLF4J 2
 * Logback + Logback logstash
 * Lognet GRPC
 * Logbook

with

 * Cloud-specific log encoders
   * GCP (Stackdriver)
 * `DevOpsLogger` extension for additional error levels 
   * Improve interaction with operations
 * Friendly logging scheme
   * Main scope (intended for deployments):
     * Machine-readable JSON 
   * Test scope (intended for local development). Log output can be toggeled at runtime:
     * Human-readable 'classic' (one-line + ANSI colors, enabled by default), or
     * Human-readable JSON (pretty-printed + ANSI colored), or
     * Machine-readable JSON (single line)
 * Request-response-logging
     * Logbook style output
     * Additional ANSI coloring for test scope
 * Selective 'on-demand' logging for unexpected web server behaviour
     * Reduce logging cost considerably while still capturing logs for problematic requests 
 * Unit testing
   * Always assert against machine-readable JSON 'under the hood', regardless what is printed to console during local development
   * Supported frameworks
     * JUnit 5
 * Correlation-id tracing
 * Custom MDC
     * gRPC

Supported web technologies:

 * Spring web
 * Spring + gRPC via Lognet
   * custom MDC support 
 * Micrometer

# License
[European Union Public Licence v1.2](https://eupl.eu/).

# Usage

## Additional error levels
Most cloud logging backends support more error levels than SFL4J. 

This library lets developers add some more details about the seriousness of the errors:

* Tell me tomorrow (default error logging)
    * Handled by devops team during next available work hours
* Interrupt my dinner
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team during wake hours
* Wake me up right now
    * Handled by devops team if within work hours, otherwise
    * Handled by operations team during wake or sleep hours

## On-demand logging
Enabled "on-demand" logging for unexpected web server behaviour.

  * Caches log statements for each request in memory, then
  * throws them away for successful responses, or
  * logs them in case of
      * failed responses (i.e. HTTP status code >= 400) and/or
      * log events of a certain level (i.e. warning or error) was made
  * since timestamps are preserved, log accumulation tools present the results in chronological order (i.e. this feature is best for deployments)
  * reduces the necessary amount of work necessary to guarantee wellformed JSON log statements for request-response logging
      * skips JSON syntax check for throw-away request-response log statements

# Cloud adaptations

## GCP
Stackdriver 

 * JSON encoder
 * Max size (for request-response log statements)

See [GCP](gcp) for further details.

# Known issues
Hosting gRPC and web services (i.e. REST services other than actuator) in the same app is not supported. 